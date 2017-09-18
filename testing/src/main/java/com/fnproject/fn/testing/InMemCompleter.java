package com.fnproject.fn.testing;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.runtime.flow.*;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In memory completer
 */
class InMemCompleter implements CompleterClient {
    private final Map<FlowId, Graph> graphs = new ConcurrentHashMap<>();
    private final AtomicInteger threadCount = new AtomicInteger();
    private final CompleterInvokeClient completerInvokeClient;
    private final FnInvokeClient fnInvokeClient;

    private static ScheduledThreadPoolExecutor spe = new ScheduledThreadPoolExecutor(1);
    private static ExecutorService faasExecutor = Executors.newCachedThreadPool();

    public interface CompleterInvokeClient {
        Result invokeStage(String fnId, FlowId flowId, CompletionId stageId, Datum.Blob closure, List<Result> body);
    }

    public interface FnInvokeClient {
        CompletableFuture<Result> invokeFunction(String fnId, HttpMethod method, Headers headers, byte[] data);
    }

    InMemCompleter(CompleterInvokeClient completerInvokeClient, FnInvokeClient fnInvokeClient) {
        this.completerInvokeClient = completerInvokeClient;
        this.fnInvokeClient = fnInvokeClient;
    }

    private ExternalCompletionServer externalCompletionServer = new ExternalCompletionServer();

    void awaitTermination() {
        while (true) {
            int aliveCount = 0;

            for (Map.Entry<FlowId, Graph> e : graphs.entrySet()) {
                if (!e.getValue().isCompleted()) {
                    aliveCount++;
                }
            }
            if (aliveCount > 0) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                }
            } else {
                break;
            }
        }
        externalCompletionServer.ensureStopped();
    }

    private static class ExternalCompletionServer {
        private static final String baseUrl = "/completions/";
        Pattern pathPattern = Pattern.compile("([^/]+)/(.*)");
        HttpServer server;
        int port = -1;

        Map<String, CompletableFuture<Result>> knownCompletions = new ConcurrentHashMap<>();


        private synchronized void ensureStopped() {
            if (server != null) {
                server.stop(0);
                server = null;
            }
        }

        private synchronized ExternalCompletionServer ensureStarted() {
            if (server != null) {
                return this;
            }
            try {
                server = HttpServer.create(new InetSocketAddress(0), 0);
                port = server.getAddress().getPort();
            } catch (IOException e) {
                throw new PlatformException("Failed to create external completer server on port " + port);
            }
            server.createContext(baseUrl, (t) -> {
                URI uri = t.getRequestURI();
                String path = uri.getPath().substring(baseUrl.length());
                Matcher match = pathPattern.matcher(path);
                if (match.matches()) {
                    String action = match.group(2);
                    String id = match.group(1);

                    CompletableFuture<Result> completableFuture = knownCompletions.get(id);
                    if (null == completableFuture) {
                        t.sendResponseHeaders(404, 0);
                        t.close();
                        return;
                    }

                    boolean success;
                    switch (action) {
                        case "complete":
                            success = true;
                            break;
                        case "fail":
                            success = false;

                            break;
                        default:
                            t.sendResponseHeaders(404, 0);
                            t.close();
                            return;
                    }

                    Map<String, String> headers = new HashMap<>();
                    for (Map.Entry<String, List<String>> e : t.getRequestHeaders().entrySet()) {
                        headers.put(e.getKey(), e.getValue().stream().collect(Collectors.joining(";")));
                    }


                    byte[] body = IOUtils.toByteArray(t.getRequestBody());
                    HttpMethod method = HttpMethod.valueOf(t.getRequestMethod().toUpperCase());

                    Datum.HttpReqDatum datum = new Datum.HttpReqDatum(method, Headers.fromMap(headers), body);
                    if (success) {
                        completableFuture.complete(Result.success(datum));
                    } else {
                        completableFuture.completeExceptionally(new ResultException(datum));
                    }
                    t.sendResponseHeaders(200, 0);
                    t.close();
                } else {
                    t.sendResponseHeaders(404, 0);
                    t.close();
                }
            });
            server.start();
            return this;
        }

        private ExternalCompletion createCompletion(FlowId tid, CompletionId cid, CompletableFuture<Result> resultFuture) {
            ensureStarted();
            String path = tid.getId() + "_" + cid.getId();

            knownCompletions.put(path, resultFuture);

            return createCompletion(cid, baseUrl, port, path);
        }

        private static ExternalCompletion createCompletion(CompletionId cid, String baseUrl, int port, String path) {
            return new ExternalCompletion() {
                @Override
                public CompletionId completionId() {
                    return cid;
                }

                @Override
                public URI completeURI() {
                    return URI.create("http://localhost:" + port + baseUrl + path + "/complete");
                }

                @Override
                public URI failureURI() {
                    return URI.create("http://localhost:" + port + baseUrl + path + "/fail");
                }
            };
        }
    }

    @Override
    public FlowId createFlow(String functionId) {
        FlowId id = new FlowId("flow-" + threadCount.incrementAndGet());
        graphs.put(id, new Graph(functionId, id));

        return id;
    }

    @Override
    public CompletionId supply(FlowId flowID, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowID, graph -> graph.addSupplyStage(serializeJava(code))).getId();
    }

    private Datum.Blob serializeJava(Object code) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(code);
            oos.close();
            return new Datum.Blob(RemoteCompleterApiClient.CONTENT_TYPE_JAVA_OBJECT, bos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            throw new LambdaSerializationException("Error serializing closure");
        }
    }

    private <T> T withActiveGraph(FlowId t, Function<Graph, T> act) {
        Graph g = graphs.get(t);
        if (g == null) {
            throw new PlatformException("unknown graph " + t.getId());
        }
        if (g.mainFinished.get()) {
            throw new PlatformException("graph already run");
        }
        return act.apply(g);
    }

    @Override
    public CompletionId thenApply(FlowId flowID, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowID,
                (graph) -> graph.withStage(completionId,
                        (parent) -> parent.addThenApplyStage(serializeJava(code)))).getId();
    }

    @Override
    public CompletionId whenComplete(FlowId flowID, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowID,
                (graph) -> graph.withStage(completionId,
                        (parent) -> parent.addWhenCompleteStage(serializeJava(code)))).getId();

    }

    @Override
    public CompletionId thenCompose(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) -> graph.withStage(completionId,
                        (parent) -> parent.addThenComposeStage(serializeJava(code)))).getId();
    }

    @Override
    public Object waitForCompletion(FlowId flowId, CompletionId completionId, ClassLoader loader) {
        return withActiveGraph(flowId, (graph) -> graph.withStage(completionId, (stage) -> {
            try {
                return stage.outputFuture().toCompletableFuture().get().toJavaObject(loader);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ResultException) {

                    Result r = ((ResultException) e.getCause()).toResult();
                    Object err = r.toJavaObject(loader);
                    if (err instanceof Throwable) {
                        throw new FlowCompletionException((Throwable) err);
                    } else if (err instanceof HttpResponse && !r.isSuccess()) {
                        throw new FlowCompletionException(new FunctionInvocationException((HttpResponse) err));
                    } else if (err instanceof HttpRequest && !r.isSuccess()) {
                        throw new FlowCompletionException(new ExternalCompletionException((HttpRequest) err));
                    }
                    throw new PlatformException(e);
                } else {
                    throw new PlatformException(e);
                }
            } catch (Exception e) {
                throw new PlatformException(e);
            }
        }));

    }

    @Override
    public Object waitForCompletion(FlowId flowId, CompletionId completionId, ClassLoader loader, long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return withActiveGraph(flowId, (graph) -> graph.withStage(completionId, (stage) -> {
                try {
                    return stage.outputFuture().toCompletableFuture().get(timeout, unit).toJavaObject(loader);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof ResultException) {

                        Result r = ((ResultException) e.getCause()).toResult();
                        Object err = r.toJavaObject(loader);
                        if (err instanceof Throwable) {
                            throw new FlowCompletionException((Throwable) err);
                        } else if (err instanceof HttpResponse && !r.isSuccess()) {
                            throw new FlowCompletionException(new FunctionInvocationException((HttpResponse) err));
                        } else if (err instanceof HttpRequest && !r.isSuccess()) {
                            throw new FlowCompletionException(new ExternalCompletionException((HttpRequest) err));
                        }
                        throw new PlatformException(e);
                    } else {
                        throw new PlatformException(e);
                    }
                } catch (Exception e) {
                    throw new PlatformException(e);
                }
            }));
        } catch (PlatformException e) {
            if (e.getCause() instanceof TimeoutException) {
                throw (TimeoutException) e.getCause();
            } else throw e;
        }
    }

    @Override
    public CompletionId thenAccept(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) -> graph.withStage(completionId,
                        (parent) -> parent.addThenAcceptStage(serializeJava(code)))).getId();
    }

    @Override
    public CompletionId thenRun(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) -> graph.withStage(completionId,
                        (parent) -> parent.addThenRunStage(serializeJava(code)))).getId();

    }

    @Override
    public CompletionId acceptEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) ->
                        graph.withStage(alternate,
                                (other) ->
                                        graph.appendChildStage(completionId,
                                                (parent) -> parent.addAcceptEitherStage(other, serializeJava(code))).getId()));

    }

    @Override
    public CompletionId applyToEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) ->
                        graph.withStage(alternate,
                                (other) ->
                                        graph.withStage(completionId,
                                                (parent) -> parent.addApplyToEitherStage(other, serializeJava(code))).getId()));
    }

    @Override
    public CompletionId anyOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) ->
                        graph.withStages(cids, graph::addAnyOf).getId());

    }

    @Override
    public CompletionId delay(FlowId flowId, long l, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) -> graph.addDelayStage(l)).getId();

    }


    @Override
    public CompletionId thenAcceptBoth(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) ->
                        graph.withStage(alternate,
                                (other) ->
                                        graph.withStage(completionId,
                                                (parent) -> parent.addThenAcceptBothStage(other, serializeJava(code))).getId()));

    }

    @Override
    public ExternalCompletion createExternalCompletion(FlowId flowId, CodeLocation codeLocation) {
        CompletableFuture<Result> resultFuture = new CompletableFuture<>();

        Graph.Stage stage = withActiveGraph(flowId,
                graph -> graph.addExternalStage(resultFuture));
        return externalCompletionServer.ensureStarted().createCompletion(flowId, stage.id, resultFuture);
    }

    @Override
    public CompletionId invokeFunction(FlowId flowId, String functionId, byte[] data, HttpMethod method, Headers headers, CodeLocation codeLocation) {
        return withActiveGraph(flowId, (graph) ->
                graph.addInvokeFunction(functionId, method, headers, data)).getId();
    }

    @Override
    public CompletionId completedValue(FlowId flowId, boolean success, Object value, CodeLocation codeLocation) {
        return withActiveGraph(flowId, (graph) ->
                graph.addCompletedValue(success, new Datum.BlobDatum(serializeJava(value)))).getId();
    }

    @Override
    public CompletionId allOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) ->
                        graph.withStages(cids, graph::addAllOf).getId());
    }

    @Override
    public CompletionId handle(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) ->
                        graph.withStage(completionId,
                                (stage) -> stage.addHandleStage(serializeJava(code))).getId());
    }

    @Override
    public CompletionId exceptionally(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) ->
                        graph.withStage(completionId,
                                (stage) -> stage.addExceptionallyStage(serializeJava(code))).getId());
    }

    @Override
    public CompletionId exceptionallyCompose(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) ->
                        graph.withStage(completionId,
                                (stage) -> stage.addExceptionallyComposeStage(serializeJava(code))).getId());
    }

    @Override
    public CompletionId thenCombine(FlowId flowId, CompletionId completionId, Serializable code, CompletionId alternate, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
                (graph) ->
                        graph.withStage(alternate,
                                (other) ->
                                        graph.appendChildStage(completionId,
                                                (parent) -> parent.addThenCombineStage(other, serializeJava(code))).getId()));

    }

    @Override
    public void commit(FlowId flowId) {
        withActiveGraph(flowId, Graph::commit);
    }

    @Override
    public void addTerminationHook(FlowId flowId, Serializable code, CodeLocation codeLocation) {
        withActiveGraph(flowId, (g) -> {
            g.addTerminationHook(serializeJava(code));

            return null;
        });
    }


    private static class TerminationHook {
        private final CompletionId id;
        private final Datum.Blob code;

        private TerminationHook(CompletionId id, Datum.Blob code) {
            this.id = id;
            this.code = code;
        }
    }

    class Graph {
        private final String functionId;
        private final FlowId graphId;
        private final AtomicBoolean committed = new AtomicBoolean(false);
        private final AtomicInteger stageCount = new AtomicInteger();
        private final AtomicInteger activeCount = new AtomicInteger();
        private final Map<CompletionId, Stage> stages = new ConcurrentHashMap<>();
        private final AtomicBoolean mainFinished = new AtomicBoolean(false);
        private final AtomicReference<Flow.FlowState> terminationSTate = new AtomicReference<>();
        private final AtomicBoolean complete = new AtomicBoolean(false);
        private final List<TerminationHook> terminationHooks = Collections.synchronizedList(new ArrayList<>());

        Graph(String functionId, FlowId graphId) {
            this.functionId = functionId;
            this.graphId = graphId;
        }

        boolean isCompleted() {
            return complete.get();
        }

        private void checkCompletion() {
            if (!mainFinished.get()) {
                if (committed.get() && activeCount.get() == 0) {
                    mainFinished.set(true);
                    workShutdown();
                }
            }
        }

        private void workShutdown() {

            if (terminationHooks.size() != 0) {
                TerminationHook hook = terminationHooks.remove(0);
                CompletableFuture.runAsync(() -> {
                    completerInvokeClient.invokeStage(functionId, graphId, hook.id, hook.code, Arrays.asList(Result.success(new Datum.StateDatum(Flow.FlowState.SUCCEEDED))));
                }).whenComplete((r, e) -> this.workShutdown());
            } else {
                complete.set(true);

            }
        }

        private boolean commit() {
            boolean commitResult = committed.compareAndSet(false, true);
            if (commitResult) {
                checkCompletion();
            }
            return commitResult;
        }

        private Optional<Stage> findStage(CompletionId ref) {
            return Optional.ofNullable(stages.get(ref));
        }

        private Stage addStage(Stage stage) {
            stages.put(stage.getId(), stage);
            return stage;
        }

        private CompletionId newStageId() {
            return new CompletionId("" + stageCount.incrementAndGet());
        }


        private Stage appendChildStage(CompletionId cid, Function<Stage, Stage> ctor) {
            Stage newStage = withStage(cid, ctor);
            stages.put(newStage.getId(), newStage);
            return newStage;
        }

        private <T> T withStage(CompletionId cid, Function<Stage, T> function) {
            Stage stage = stages.get(cid);
            if (stage == null) {
                throw new PlatformException("Stage not found in graph :" + cid);
            }
            return function.apply(stage);
        }

        private <T> T withStages(List<CompletionId> cids, Function<List<Stage>, T> function) {
            List<Stage> stages = new ArrayList<>();
            for (CompletionId cid : cids) {
                Stage stage = this.stages.get(cid);
                if (stage == null) {
                    throw new PlatformException("Stage not  found in graph :" + cid);
                }
                stages.add(stage);
            }
            return function.apply(stages);
        }

        private Stage addCompletedValue(boolean success, Datum value) {
            CompletableFuture<Result> future;

            if (success) {
                future = CompletableFuture.completedFuture(Result.success(value));
            } else {
                future = new CompletableFuture<>();
                future.completeExceptionally(new ResultException(value));
            }
            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
                    (x, f) -> future));
        }

        private Stage addAllOf(List<Stage> cns) {
            List<CompletableFuture<Result>> outputs = cns.stream().map(Stage::outputFuture).map(CompletionStage::toCompletableFuture).collect(Collectors.toList());

            CompletionStage<Result> output = CompletableFuture
                    .allOf(outputs.toArray(new CompletableFuture<?>[outputs.size()]))
                    .thenApply((nv) -> Result.success(new Datum.EmptyDatum()));

            return addStage(new Stage(
                    CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, f) -> output));

        }

        private Stage addAnyOf(List<Stage> cns) {
            List<CompletableFuture<Result>> outputs = cns.stream().map(Stage::outputFuture).map(CompletionStage::toCompletableFuture).collect(Collectors.toList());

            CompletionStage<Result> output = CompletableFuture
                    .anyOf(outputs.toArray(new CompletableFuture<?>[outputs.size()])).thenApply((s) -> (Result) s);

            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, x) -> output
            ));
        }

        private Stage addSupplyStage(Datum.Blob closure) {
            CompletableFuture<List<Result>> input = CompletableFuture.completedFuture(Collections.emptyList());
            return addStage(new Stage(input, chainInvocation(closure)));
        }


        private Stage addExternalStage(CompletableFuture<Result> future) {
            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, v) -> future));
        }

        private Stage addDelayStage(long delay) {
            CompletableFuture<Result> future = new CompletableFuture<>();

            spe.schedule(() -> future.complete(Result.success(new Datum.EmptyDatum())), delay, TimeUnit.MILLISECONDS);

            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, v) -> future));
        }

        private Stage addInvokeFunction(String functionId, HttpMethod method, Headers headers, byte[] data) {
            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, in) -> in.thenComposeAsync((ignored) ->
                            fnInvokeClient.invokeFunction(functionId, method, headers, data), faasExecutor)));
        }

        private BiFunction<Stage, CompletionStage<List<Result>>, CompletionStage<Result>> chainInvocation(Datum.Blob closure) {
            return (stage, trigger) -> trigger.thenApplyAsync((input) -> {
                return completerInvokeClient.invokeStage(functionId, graphId, stage.id, closure, input);
            }, faasExecutor);
        }

        private void addTerminationHook(Datum.Blob closure) {
            this.terminationHooks.add(0, new TerminationHook(newStageId(), closure));

        }


        private final class Stage {
            private final CompletionId id;
            private final CompletionStage<Result> outputFuture;


            private Stage(CompletionStage<List<Result>> input,
                          BiFunction<Stage, CompletionStage<List<Result>>, CompletionStage<Result>> invoke) {

                this.id = newStageId();
                input.whenComplete((in, err) -> activeCount.incrementAndGet());
                this.outputFuture = invoke.apply(this, input);
                outputFuture.whenComplete((in, err) -> {
                    activeCount.decrementAndGet();
                    checkCompletion();
                });
            }

            private CompletionStage<Result> outputFuture() {
                return outputFuture;
            }

            private CompletionId getId() {
                return id;
            }

            private Stage addThenApplyStage(Datum.Blob closure) {
                return addStage(new Stage(
                        outputFuture().thenApply(Collections::singletonList),
                        chainInvocation(closure)
                ));
            }

            private Stage addThenAcceptStage(Datum.Blob closure) {
                return addStage(new Stage(
                        outputFuture().thenApply(Collections::singletonList),
                        chainInvocation(closure)
                ));
            }

            private Stage addThenRunStage(Datum.Blob closure) {
                return addStage(new Stage(
                        outputFuture().thenApply(Collections::singletonList),
                        chainInvocation(closure)
                ));
            }

            private Stage addThenComposeStage(Datum.Blob closure) {
                BiFunction<Stage, CompletionStage<List<Result>>, CompletionStage<Result>> invokefn =
                        chainInvocation(closure)
                                .andThen((resultStage) -> resultStage.thenCompose(this::composeResultStage));

                return addStage(new Stage(outputFuture().thenApply(Collections::singletonList), invokefn
                ));
            }


            private List<Result> resultOrError(Result input, Throwable err) {
                if (err != null) {
                    return Arrays.asList(Result.success(new Datum.EmptyDatum()), errorToResult(err));
                } else {
                    return Arrays.asList(input, Result.success(new Datum.EmptyDatum()));
                }
            }

            private Result errorToResult(Throwable err) {
                if (err.getCause() instanceof ResultException) {
                    return ((ResultException) err.getCause()).toResult();
                } else {
                    return Result.failure(new Datum.ErrorDatum(Datum.ErrorType.unknown_error, "Unexpected error " + err.getMessage()));
                }
            }

            private Stage addWhenCompleteStage(Datum.Blob closure) {
                // This is quite fiddly - because the semantics of whenComplete is.
                // We need to construct a new completable future that can be completed only once the whenComplete continuation
                // has been finished.
                return addStage(new Stage(outputFuture().thenApply(Collections::singletonList),  // result or exception
                        (stage, inputs) -> {
                            CompletableFuture<Result> cf = new CompletableFuture<>();
                            inputs.whenComplete((results, err) -> {
                                // We have ([result], null) or (null, CompletionException(ResultException(datum=exception)))
                                // In the latter case, resultOrError will take err as-is.
                                Result result = results != null ? results.get(0) : null;
                                chainInvocation(closure).apply(stage, CompletableFuture.completedFuture(resultOrError(result, err)))
                                        .whenComplete((r, e2) -> {   // Throw away the result of the whenComplete lambda
                                            if (err != null) {
                                                cf.completeExceptionally(err);
                                            } else {
                                                cf.complete(result);
                                            }

                                        });
                            });
                            return cf;
                        }));

            }

            private Stage addHandleStage(Datum.Blob closure) {
                return addStage(new Stage(outputFuture().handle(this::resultOrError)
                        , chainInvocation(closure)
                ));
            }

            private Stage addAcceptEitherStage(Stage otherStage, Datum.Blob closure) {
                return addStage(new Stage(
                        outputFuture().applyToEither(otherStage.outputFuture, Function.identity())
                                .thenApply(Collections::singletonList),
                        chainInvocation(closure)
                                .andThen(c -> c.thenApply(Result::toEmpty))
                ));
            }

            private Stage addApplyToEitherStage(Stage otherStage, Datum.Blob closure) {
                return addStage(new Stage(
                        outputFuture().applyToEither(otherStage.outputFuture, Collections::singletonList),
                        chainInvocation(closure)
                ));
            }

            private Stage addThenAcceptBothStage(Stage otherStage, Datum.Blob closure) {
                return addStage(new Stage(outputFuture()
                        .thenCombine(otherStage.outputFuture,
                                (input1, input2) -> Arrays.asList(input1, input2)),
                        chainInvocation(closure)
                                .andThen(c -> c.thenApply(Result::toEmpty))
                ));

            }

            private Stage addExceptionallyStage(Datum.Blob closure) {
                return addStage(new Stage(outputFuture().thenApply(Collections::singletonList),
                        (stage, inputs) -> {
                            CompletableFuture<Result> result = new CompletableFuture<>();
                            inputs.whenComplete((results, err) -> {
                                if (err != null) {
                                    if (err instanceof CompletionException && err.getCause() instanceof ResultException) {
                                        chainInvocation(closure).apply(stage, CompletableFuture.completedFuture(Collections.singletonList(((ResultException) err.getCause()).toResult())))
                                                .whenComplete((r, e) -> {
                                                    if (e != null) {
                                                        result.completeExceptionally(err);
                                                    } else {
                                                        result.complete(r);
                                                    }
                                                });
                                    } else {
                                        result.completeExceptionally(new ResultException(new Datum.ErrorDatum(Datum.ErrorType.unknown_error, "Unexpected error" + err.getMessage())));
                                    }
                                } else {
                                    result.complete(results.get(0));
                                }
                            });
                            return result;
                        }));
            }

            private Stage addThenCombineStage(Stage otherStage, Datum.Blob closure) {
                return addStage(new Stage(outputFuture()
                        .thenCombine(otherStage.outputFuture,
                                (input1, input2) -> Arrays.asList(input1, input2)),
                        chainInvocation(closure)
                ));
            }

            private BiConsumer<Result, Throwable> mapToResult(CompletableFuture<Result> result) {
                return (innerResult, innerErr) -> {
                    if (innerErr != null) {
                        if (innerErr instanceof CompletionException && innerErr.getCause() instanceof ResultException) {
                            result.completeExceptionally(((ResultException) innerErr.getCause()));

                        } else {
                            result.completeExceptionally(new ResultException(new Datum.ErrorDatum(Datum.ErrorType.unknown_error, "Unexpected error" + innerErr.getMessage())));
                        }
                    } else {
                        result.complete(innerResult);
                    }
                };
            }

            public Stage addExceptionallyComposeStage(Datum.Blob blob) {
                CompletableFuture<Result> result = new CompletableFuture<>();

                BiFunction<Stage, CompletionStage<List<Result>>, CompletionStage<Result>> invoke =
                        (stage, inputs) -> {
                            inputs.whenComplete((v, err) -> {
                                if (err != null) {
                                    if (err instanceof CompletionException && err.getCause() instanceof ResultException) {
                                        chainInvocation(blob)
                                                .apply(stage, CompletableFuture.completedFuture(Arrays.asList(((ResultException) err.getCause()).toResult())))
                                                .thenCompose(this::composeResultStage).whenComplete(mapToResult(result));

                                    } else {
                                        result.completeExceptionally(new ResultException(new Datum.ErrorDatum(Datum.ErrorType.unknown_error, "Unexpected error" + err.getMessage())));
                                    }
                                } else {
                                    result.complete(v.get(0));
                                }
                            });

                            return result;
                        };
                return addStage(new Stage(outputFuture().thenApply(Collections::singletonList), invoke));
            }

            private CompletionStage<Result> composeResultStage(Result r) {
                if (r.getDatum() instanceof Datum.StageRefDatum) {
                    String ref = ((Datum.StageRefDatum) r.getDatum()).getStageId();

                    Stage otherStage = findStage(new CompletionId(ref)).orElseThrow(() ->
                            new ResultException(new Datum.ErrorDatum(Datum.ErrorType.invalid_stage_response, "returned stage not found")));
                    return otherStage.outputFuture;
                } else {
                    throw new ResultException(new Datum.ErrorDatum(Datum.ErrorType.invalid_stage_response, "Result was not a stageref datum"));
                }
            }
        }
    }
}
