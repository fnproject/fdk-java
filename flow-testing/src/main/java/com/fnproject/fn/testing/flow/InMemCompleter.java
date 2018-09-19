package com.fnproject.fn.testing.flow;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;
import com.fnproject.fn.runtime.flow.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * In memory completer
 */
class InMemCompleter implements CompleterClient, BlobStoreClient, CompleterClientFactory {
    private final Map<FlowId, Flow> graphs = new ConcurrentHashMap<>();
    private final AtomicInteger threadCount = new AtomicInteger();
    private final CompleterInvokeClient completerInvokeClient;
    private final FnInvokeClient fnInvokeClient;

    private static ScheduledThreadPoolExecutor spe = new ScheduledThreadPoolExecutor(1);
    private static ExecutorService faasExecutor = Executors.newCachedThreadPool();

    @Override
    public BlobResponse writeBlob(String prefix, byte[] bytes, String contentType) {
        FlowId flow = new FlowId(prefix);
        if (!graphs.containsKey(flow)) {
            throw new IllegalStateException("flow " + flow + " does not exist");
        }


        String blobId = UUID.randomUUID().toString();
        Blob blob = new Blob(bytes, contentType);

        graphs.get(flow).blobs.put(blobId, blob);
        BlobResponse returnBlob = new BlobResponse();
        returnBlob.blobId = blobId;
        returnBlob.contentType = contentType;
        returnBlob.blobLength = (long) bytes.length;
        return returnBlob;
    }

    @Override
    public <T> T readBlob(String prefix, String blobId, Function<InputStream, T> reader, String expectedContentType) {
        FlowId flow = new FlowId(prefix);
        if (!graphs.containsKey(flow)) {
            throw new IllegalStateException("flow " + flow + " does not exist");
        }
        Blob blob = graphs.get(flow).blobs.get(blobId);
        if (blob == null) {
            throw new IllegalStateException("Blob " + blobId + " not found");
        }

        if (!blob.contentType.equals(expectedContentType)) {
            throw new IllegalStateException("Blob content type mismatch");
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(blob.data);

        return reader.apply(bis);
    }

    @Override
    public CompleterClient getCompleterClient() {
        return this;
    }

    @Override
    public BlobStoreClient getBlobStoreClient() {
        return this;
    }

    public interface CompleterInvokeClient {
        APIModel.CompletionResult invokeStage(String fnId, FlowId flowId, CompletionId stageId, APIModel.Blob closure, List<APIModel.CompletionResult> body);
    }

    public interface FnInvokeClient {
        CompletableFuture<HttpResponse> invokeFunction(String fnId, HttpMethod method, Headers headers, byte[] data);
    }

    InMemCompleter(CompleterInvokeClient completerInvokeClient, FnInvokeClient fnInvokeClient) {
        this.completerInvokeClient = completerInvokeClient;
        this.fnInvokeClient = fnInvokeClient;
    }

    void awaitTermination() {
        while (true) {
            int aliveCount = 0;

            for (Map.Entry<FlowId, Flow> e : graphs.entrySet()) {
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
    }


    @Override
    public FlowId createFlow(String functionId) {
        FlowId id = new FlowId("flow-" + threadCount.incrementAndGet());
        graphs.put(id, new Flow(functionId, id));

        return id;
    }

    @Override
    public CompletionId supply(FlowId flowID, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowID, flow -> flow.addSupplyStage(serializeJava(flowID, code))).getId();
    }

    private APIModel.Blob serializeJava(FlowId flowId, Object code) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(code);
            oos.close();
            BlobResponse blobResponse = writeBlob(flowId.getId(), bos.toByteArray(), RemoteFlowApiClient.CONTENT_TYPE_JAVA_OBJECT);

            return APIModel.Blob.fromBlobResponse(blobResponse);
        } catch (Exception e) {
            e.printStackTrace();
            throw new LambdaSerializationException("Error serializing closure");
        }
    }

    private <T> T withActiveGraph(FlowId t, Function<Flow, T> act) {
        Flow g = graphs.get(t);
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
           (flow) -> flow.withStage(completionId,
              (parent) -> parent.addThenApplyStage(serializeJava(flowID, code)))).getId();
    }

    @Override
    public CompletionId whenComplete(FlowId flowID, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowID,
           (flow) -> flow.withStage(completionId,
              (parent) -> parent.addWhenCompleteStage(serializeJava(flowID, code)))).getId();

    }

    @Override
    public CompletionId thenCompose(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) -> flow.withStage(completionId,
              (parent) -> parent.addThenComposeStage(serializeJava(flowId, code)))).getId();
    }

    private <T> T doInClassLoader(ClassLoader cl, Callable<T> call) throws Exception {
        ClassLoader myCL = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(cl);
            return call.call();
        } finally {
            Thread.currentThread().setContextClassLoader(myCL);
        }
    }

    @Override
    public Object waitForCompletion(FlowId flowId, CompletionId completionId, ClassLoader loader) {
        return withActiveGraph(flowId, (flow) -> flow.withStage(completionId, (stage) -> {
            try {
                return stage.outputFuture().toCompletableFuture().get().toJava(flowId, this, loader);

            } catch (ExecutionException e) {
                if (e.getCause() instanceof ResultException) {

                    APIModel.CompletionResult r = ((ResultException) e.getCause()).toResult();
                    Object err = r.toJava(flowId, this, loader);
                    if (err instanceof Throwable) {
                        throw new FlowCompletionException((Throwable) err);
                    } else if (err instanceof HttpResponse && !r.successful) {
                        throw new FlowCompletionException(new FunctionInvocationException((HttpResponse) err));
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
            return withActiveGraph(flowId, (flow) -> flow.withStage(completionId, (stage) -> {
                try {
                    ClassLoader myCL = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(loader);
                        return stage.outputFuture().toCompletableFuture().get().toJava(flowId, this, loader);
                    } finally {
                        Thread.currentThread().setContextClassLoader(myCL);
                    }
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof ResultException) {

                        APIModel.CompletionResult r = ((ResultException) e.getCause()).toResult();
                        Object err = r.toJava(flowId, this, loader);
                        if (err instanceof Throwable) {
                            throw new FlowCompletionException((Throwable) err);
                        } else if (err instanceof HttpResponse && !r.successful) {
                            throw new FlowCompletionException(new FunctionInvocationException((HttpResponse) err));
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
           (flow) -> flow.withStage(completionId,
              (parent) -> parent.addThenAcceptStage(serializeJava(flowId, code)))).getId();
    }

    @Override
    public CompletionId thenRun(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) -> flow.withStage(completionId,
              (parent) -> parent.addThenRunStage(serializeJava(flowId, code)))).getId();

    }

    @Override
    public CompletionId acceptEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) ->
              flow.withStage(alternate,
                 (other) ->
                    flow.appendChildStage(completionId,
                       (parent) -> parent.addAcceptEitherStage(other, serializeJava(flowId, code))).getId()));

    }

    @Override
    public CompletionId applyToEither(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) ->
              flow.withStage(alternate,
                 (other) ->
                    flow.withStage(completionId,
                       (parent) -> parent.addApplyToEitherStage(other, serializeJava(flowId, code))).getId()));
    }

    @Override
    public boolean complete(FlowId flowId, CompletionId completionId, Object value, CodeLocation codeLocation) {
        return withActiveGraph(flowId, (flow) -> flow.withStage(completionId, (stage) -> stage.complete(APIModel.BlobDatum.fromBlob(serializeJava(flowId, value)))));
    }

    @Override
    public boolean completeExceptionally(FlowId flowId, CompletionId completionId, Throwable value, CodeLocation codeLocation) {
        return withActiveGraph(flowId, (flow) -> flow.withStage(completionId, (stage) -> stage.completeExceptionally(APIModel.BlobDatum.fromBlob(serializeJava(flowId, value)))));
    }

    @Override
    public CompletionId anyOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) ->
              flow.withStages(cids, flow::addAnyOf).getId());

    }

    @Override
    public CompletionId delay(FlowId flowId, long l, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) -> flow.addDelayStage(l)).getId();

    }


    @Override
    public CompletionId thenAcceptBoth(FlowId flowId, CompletionId completionId, CompletionId alternate, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) ->
              flow.withStage(alternate,
                 (other) ->
                    flow.withStage(completionId,
                       (parent) -> parent.addThenAcceptBothStage(other, serializeJava(flowId, code))).getId()));

    }

    @Override
    public CompletionId createCompletion(FlowId flowId, CodeLocation codeLocation) {
        CompletableFuture<APIModel.CompletionResult> resultFuture = new CompletableFuture<>();

        Flow.Stage stage = withActiveGraph(flowId,
           flow -> flow.addExternalStage(resultFuture));

        return stage.getId();
    }

    @Override
    public CompletionId invokeFunction(FlowId flowId, String functionId, byte[] data, HttpMethod method, Headers headers, CodeLocation codeLocation) {
        return withActiveGraph(flowId, (flow) ->
           flow.addInvokeFunction(functionId, method, headers, data)).getId();
    }

    @Override
    public CompletionId completedValue(FlowId flowId, boolean success, Object value, CodeLocation codeLocation) {
        return withActiveGraph(flowId, (flow) ->
           flow.addCompletedValue(success, APIModel.BlobDatum.fromBlob(serializeJava(flowId, value)))).getId();
    }

    @Override
    public CompletionId allOf(FlowId flowId, List<CompletionId> cids, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) ->
              flow.withStages(cids, flow::addAllOf).getId());
    }

    @Override
    public CompletionId handle(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) ->
              flow.withStage(completionId,
                 (stage) -> stage.addHandleStage(serializeJava(flowId, code))).getId());
    }

    @Override
    public CompletionId exceptionally(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) ->
              flow.withStage(completionId,
                 (stage) -> stage.addExceptionallyStage(serializeJava(flowId, code))).getId());
    }

    @Override
    public CompletionId exceptionallyCompose(FlowId flowId, CompletionId completionId, Serializable code, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) ->
              flow.withStage(completionId,
                 (stage) -> stage.addExceptionallyComposeStage(serializeJava(flowId, code))).getId());
    }

    @Override
    public CompletionId thenCombine(FlowId flowId, CompletionId completionId, Serializable code, CompletionId alternate, CodeLocation codeLocation) {
        return withActiveGraph(flowId,
           (flow) ->
              flow.withStage(alternate,
                 (other) ->
                    flow.appendChildStage(completionId,
                       (parent) -> parent.addThenCombineStage(other, serializeJava(flowId, code))).getId()));

    }

    @Override
    public void commit(FlowId flowId) {
        withActiveGraph(flowId, Flow::commit);
    }

    @Override
    public void addTerminationHook(FlowId flowId, Serializable code, CodeLocation codeLocation) {
        withActiveGraph(flowId, (g) -> {
            g.addTerminationHook(serializeJava(flowId, code));

            return null;
        });
    }


    private static class TerminationHook {
        private final CompletionId id;
        private final APIModel.Blob code;

        private TerminationHook(CompletionId id, APIModel.Blob code) {
            this.id = id;
            this.code = code;
        }
    }

    class Blob {
        private final byte[] data;
        private final String contentType;

        Blob(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }
    }

    class Flow {
        private final Map<String, Blob> blobs = new HashMap<>();

        private final String functionId;
        private final FlowId flowId;
        private final AtomicBoolean committed = new AtomicBoolean(false);
        private final AtomicInteger stageCount = new AtomicInteger();
        private final AtomicInteger activeCount = new AtomicInteger();
        private final Map<CompletionId, Stage> stages = new ConcurrentHashMap<>();
        private final AtomicBoolean mainFinished = new AtomicBoolean(false);
        private final AtomicReference<com.fnproject.fn.api.flow.Flow.FlowState> terminationSTate = new AtomicReference<>();
        private final AtomicBoolean complete = new AtomicBoolean(false);
        private final List<TerminationHook> terminationHooks = Collections.synchronizedList(new ArrayList<>());

        Flow(String functionId, FlowId flowId) {
            this.functionId = functionId;
            this.flowId = flowId;
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
                    completerInvokeClient.invokeStage(functionId, flowId, hook.id, hook.code,
                       Collections.singletonList(APIModel.CompletionResult.success(APIModel.StatusDatum.fromType(APIModel.StatusDatumType.Succeeded))));
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

        private Stage addCompletedValue(boolean success, APIModel.Datum value) {
            CompletableFuture<APIModel.CompletionResult> future;

            if (success) {
                future = CompletableFuture.completedFuture(APIModel.CompletionResult.success(value));
            } else {
                future = new CompletableFuture<>();
                future.completeExceptionally(new ResultException(value));
            }
            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
               (x, f) -> future));
        }

        private Stage addAllOf(List<Stage> cns) {
            List<CompletableFuture<APIModel.CompletionResult>> outputs = cns.stream().map(Stage::outputFuture).map(CompletionStage::toCompletableFuture).collect(Collectors.toList());

            CompletionStage<APIModel.CompletionResult> output = CompletableFuture
               .allOf(outputs.toArray(new CompletableFuture<?>[outputs.size()]))
               .thenApply((nv) -> APIModel.CompletionResult.success(new APIModel.EmptyDatum()));

            return addStage(new Stage(
               CompletableFuture.completedFuture(Collections.emptyList()),
               (n, f) -> output));

        }

        private Stage addAnyOf(List<Stage> cns) {
            List<CompletableFuture<APIModel.CompletionResult>> outputs = cns.stream().map(Stage::outputFuture).map(CompletionStage::toCompletableFuture).collect(Collectors.toList());

            CompletionStage<APIModel.CompletionResult> output = CompletableFuture
               .anyOf(outputs.toArray(new CompletableFuture<?>[outputs.size()])).thenApply((s) -> (APIModel.CompletionResult) s);

            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
               (n, x) -> output
            ));
        }

        private Stage addSupplyStage(APIModel.Blob closure) {
            CompletableFuture<List<APIModel.CompletionResult>> input = CompletableFuture.completedFuture(Collections.emptyList());
            return addStage(new Stage(input, chainInvocation(closure)));
        }


        private Stage addExternalStage(CompletableFuture<APIModel.CompletionResult> future) {
            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
               (n, v) -> future));
        }

        private Stage addDelayStage(long delay) {
            CompletableFuture<APIModel.CompletionResult> future = new CompletableFuture<>();

            spe.schedule(() -> future.complete(APIModel.CompletionResult.success(new APIModel.EmptyDatum())), delay, TimeUnit.MILLISECONDS);

            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
               (n, v) -> future));
        }

        private Stage addInvokeFunction(String functionId, HttpMethod method, Headers headers, byte[] data) {
            return addStage(new Stage(CompletableFuture.completedFuture(Collections.emptyList()),
               (n, in) -> {
                   return in.thenComposeAsync((ignored) -> {

                       CompletionStage<HttpResponse> respFuture = fnInvokeClient.invokeFunction(functionId, method, headers, data);
                       return respFuture.thenApply((res) -> {
                           APIModel.HTTPResp apiResp = new APIModel.HTTPResp();
                           List<APIModel.HTTPHeader> callHeaders = new ArrayList<>();

                           for (Map.Entry<String, List<String>> e : res.getHeaders().asMap().entrySet()) {
                               for (String v : e.getValue()) {
                                   callHeaders.add(APIModel.HTTPHeader.create(e.getKey(), v));
                               }
                           }
                           apiResp.headers = callHeaders;
                           BlobResponse blobResponse = writeBlob(flowId.getId(), res.getBodyAsBytes(), res.getHeaders().get("Content-type").orElse("application/octet-stream"));

                           apiResp.body = APIModel.Blob.fromBlobResponse(blobResponse);
                           apiResp.statusCode = res.getStatusCode();

                           APIModel.HTTPRespDatum datum = APIModel.HTTPRespDatum.create(apiResp);

                           if (apiResp.statusCode >= 200 && apiResp.statusCode < 400) {
                               return APIModel.CompletionResult.success(datum);
                           } else {
                               throw new ResultException(datum);
                           }

                       }).exceptionally(e -> {
                           if (e.getCause() instanceof ResultException) {
                               throw (ResultException) e.getCause();
                           } else {
                               throw new ResultException(APIModel.ErrorDatum.newError(APIModel.ErrorType.FunctionInvokeFailed, e.getMessage()));
                           }
                       });


                   }, faasExecutor);
               }

            ));
        }

        private BiFunction<Stage, CompletionStage<List<APIModel.CompletionResult>>, CompletionStage<APIModel.CompletionResult>> chainInvocation(APIModel.Blob closure) {
            return (stage, trigger) -> trigger.thenApplyAsync((input) -> {
                return completerInvokeClient.invokeStage(functionId, flowId, stage.id, closure, input);
            }, faasExecutor);
        }

        private void addTerminationHook(APIModel.Blob closure) {
            this.terminationHooks.add(0, new TerminationHook(newStageId(), closure));

        }


        private final class Stage {
            private final CompletionId id;
            private final CompletionStage<APIModel.CompletionResult> outputFuture;


            private Stage(CompletionStage<List<APIModel.CompletionResult>> input,
                          BiFunction<Stage, CompletionStage<List<APIModel.CompletionResult>>, CompletionStage<APIModel.CompletionResult>> invoke) {

                this.id = newStageId();
                input.whenComplete((in, err) -> activeCount.incrementAndGet());
                this.outputFuture = invoke.apply(this, input);
                outputFuture.whenComplete((in, err) -> {
                    activeCount.decrementAndGet();
                    checkCompletion();
                });
            }

            private CompletionStage<APIModel.CompletionResult> outputFuture() {
                return outputFuture;
            }

            private CompletionId getId() {
                return id;
            }

            private boolean complete(APIModel.BlobDatum value) {
                return outputFuture.toCompletableFuture().complete(APIModel.CompletionResult.success(value));
            }

            private boolean completeExceptionally(APIModel.BlobDatum value) {
                return outputFuture.toCompletableFuture().completeExceptionally(new ResultException(value));
            }

            private Stage addThenApplyStage(APIModel.Blob closure) {
                return addStage(new Stage(
                   outputFuture().thenApply(Collections::singletonList),
                   chainInvocation(closure)
                ));
            }

            private Stage addThenAcceptStage(APIModel.Blob closure) {
                return addStage(new Stage(
                   outputFuture().thenApply(Collections::singletonList),
                   chainInvocation(closure)
                ));
            }

            private Stage addThenRunStage(APIModel.Blob closure) {
                return addStage(new Stage(
                   outputFuture().thenApply((r)->Collections.emptyList()),
                   chainInvocation(closure)
                ));
            }

            private Stage addThenComposeStage(APIModel.Blob closure) {
                BiFunction<Stage, CompletionStage<List<APIModel.CompletionResult>>, CompletionStage<APIModel.CompletionResult>> invokefn =
                   chainInvocation(closure)
                      .andThen((resultStage) -> resultStage.thenCompose(this::composeResultStage));

                return addStage(new Stage(outputFuture().thenApply(Collections::singletonList), invokefn
                ));
            }


            private List<APIModel.CompletionResult> resultOrError(APIModel.CompletionResult input, Throwable err) {
                if (err != null) {
                    return Arrays.asList(APIModel.CompletionResult.success(new APIModel.EmptyDatum()), errorToResult(err));
                } else {
                    return Arrays.asList(input, APIModel.CompletionResult.success(new APIModel.EmptyDatum()));
                }
            }

            private APIModel.CompletionResult errorToResult(Throwable err) {
                if (err.getCause() instanceof ResultException) {
                    return ((ResultException) err.getCause()).toResult();
                } else {
                    return APIModel.CompletionResult.failure(APIModel.ErrorDatum.newError(APIModel.ErrorType.UnknownError, "Unexpected error " + err.getMessage()));
                }
            }

            private Stage addWhenCompleteStage(APIModel.Blob closure) {
                // This is quite fiddly - because the semantics of whenComplete is.
                // We need to construct a new completable future that can be completed only once the whenComplete continuation
                // has been finished.
                return addStage(new Stage(outputFuture().thenApply(Collections::singletonList),  // result or exception
                   (stage, inputs) -> {
                       CompletableFuture<APIModel.CompletionResult> cf = new CompletableFuture<>();
                       inputs.whenComplete((results, err) -> {
                           // We have ([result], null) or (null, CompletionException(ResultException(datum=exception)))
                           // In the latter case, resultOrError will take err as-is.
                           APIModel.CompletionResult result = results != null ? results.get(0) : null;
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

            private Stage addHandleStage(APIModel.Blob closure) {
                return addStage(new Stage(outputFuture().handle(this::resultOrError)
                   , chainInvocation(closure)
                ));
            }

            private APIModel.CompletionResult toEmpty(APIModel.CompletionResult res) {
                return APIModel.CompletionResult.success(new APIModel.EmptyDatum());
            }

            private Stage addAcceptEitherStage(Stage otherStage, APIModel.Blob closure) {
                return addStage(new Stage(
                   outputFuture().applyToEither(otherStage.outputFuture, Function.identity())
                      .thenApply(Collections::singletonList),
                   chainInvocation(closure)
                      .andThen(c -> c.thenApply(this::toEmpty))
                ));
            }

            private Stage addApplyToEitherStage(Stage otherStage, APIModel.Blob closure) {
                return addStage(new Stage(
                   outputFuture().applyToEither(otherStage.outputFuture, Collections::singletonList),
                   chainInvocation(closure)
                ));
            }

            private Stage addThenAcceptBothStage(Stage otherStage, APIModel.Blob closure) {
                return addStage(new Stage(outputFuture()
                   .thenCombine(otherStage.outputFuture,
                      (input1, input2) -> Arrays.asList(input1, input2)),
                   chainInvocation(closure)
                      .andThen(c -> c.thenApply(this::toEmpty))
                ));

            }

            private Stage addExceptionallyStage(APIModel.Blob closure) {
                return addStage(new Stage(outputFuture().thenApply(Collections::singletonList),
                   (stage, inputs) -> {
                       CompletableFuture<APIModel.CompletionResult> result = new CompletableFuture<>();
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
                                   result.completeExceptionally(new ResultException(APIModel.ErrorDatum.newError(APIModel.ErrorType.UnknownError, "Unexpected error" + err.getMessage())));
                               }
                           } else {
                               result.complete(results.get(0));
                           }
                       });
                       return result;
                   }));
            }

            private Stage addThenCombineStage(Stage otherStage, APIModel.Blob closure) {
                return addStage(new Stage(outputFuture()
                   .thenCombine(otherStage.outputFuture,
                      (input1, input2) -> Arrays.asList(input1, input2)),
                   chainInvocation(closure)
                ));
            }

            private BiConsumer<APIModel.CompletionResult, Throwable> mapToResult(CompletableFuture<APIModel.CompletionResult> result) {
                return (innerResult, innerErr) -> {
                    if (innerErr != null) {
                        if (innerErr instanceof CompletionException && innerErr.getCause() instanceof ResultException) {
                            result.completeExceptionally(((ResultException) innerErr.getCause()));

                        } else {
                            result.completeExceptionally(new ResultException(APIModel.ErrorDatum.newError(APIModel.ErrorType.UnknownError, "Unexpected error" + innerErr.getMessage())));
                        }
                    } else {
                        result.complete(innerResult);
                    }
                };
            }

            public Stage addExceptionallyComposeStage(APIModel.Blob blob) {
                CompletableFuture<APIModel.CompletionResult> result = new CompletableFuture<>();

                BiFunction<Stage, CompletionStage<List<APIModel.CompletionResult>>, CompletionStage<APIModel.CompletionResult>> invoke =
                   (stage, inputs) -> {
                       inputs.whenComplete((v, err) -> {
                           if (err != null) {
                               if (err instanceof CompletionException && err.getCause() instanceof ResultException) {
                                   chainInvocation(blob)
                                      .apply(stage, CompletableFuture.completedFuture(Arrays.asList(((ResultException) err.getCause()).toResult())))
                                      .thenCompose(this::composeResultStage).whenComplete(mapToResult(result));

                               } else {
                                   result.completeExceptionally(new ResultException(APIModel.ErrorDatum.newError(APIModel.ErrorType.UnknownError, "Unexpected error" + err.getMessage())));
                               }
                           } else {
                               result.complete(v.get(0));
                           }
                       });

                       return result;
                   };
                return addStage(new Stage(outputFuture().thenApply(Collections::singletonList), invoke));
            }

            private CompletionStage<APIModel.CompletionResult> composeResultStage(APIModel.CompletionResult r) {
                if (r.result instanceof APIModel.StageRefDatum) {
                    String ref = ((APIModel.StageRefDatum) r.result).stageId;

                    Stage otherStage = findStage(new CompletionId(ref)).orElseThrow(() ->
                       new ResultException(APIModel.ErrorDatum.newError(APIModel.ErrorType.InvalidStageResponse, "returned stage not found")));
                    return otherStage.outputFuture;
                } else {
                    throw new ResultException(APIModel.ErrorDatum.newError(APIModel.ErrorType.InvalidStageResponse, "Result was not a stageref datum"));
                }
            }
        }
    }
}
