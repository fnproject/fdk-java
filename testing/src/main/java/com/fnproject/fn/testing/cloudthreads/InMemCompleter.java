package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.HttpMethod;
import com.fnproject.fn.runtime.cloudthreads.CompleterClient;
import com.fnproject.fn.runtime.cloudthreads.CompletionId;
import com.fnproject.fn.runtime.cloudthreads.TestSupport;
import com.fnproject.fn.runtime.cloudthreads.ThreadId;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created on 25/08/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class InMemCompleter implements CompleterClient {
    private final Map<ThreadId, Graph> graphs = new ConcurrentHashMap<>();
    private final AtomicInteger threadCount = new AtomicInteger();
    private final FaaSInvokeClient client;
    private static ScheduledThreadPoolExecutor spe = new ScheduledThreadPoolExecutor(1);

    public InMemCompleter(FaaSInvokeClient client) {
        this.client = client;
    }

    @Override
    public ThreadId createThread(String functionId) {
        ThreadId id = TestSupport.threadId("thread-" + threadCount.incrementAndGet());

        graphs.put(id, new Graph(id, functionId));

        return id;
    }

    @Override
    public CompletionId supply(ThreadId threadID, Serializable code) {
        Graph graph = Objects.requireNonNull(graphs.get(threadID), "Unknown graph");
        Graph.Node supply = graph.supply(serializeClosure(code));
        graph.addRootNode(supply);
        return supply.getId();

    }

    private Datum.Blob serializeClosure(Serializable code) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(code);
            oos.close();
            return new Datum.Blob("application/x-java-serialized-object", bos.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Error serializing closure ", e);
        }
    }

    private <T> T withGraph(ThreadId t, Function<Graph, T> act) {
        Graph g = graphs.get(t);
        if (g == null) {
            throw new RuntimeException("unknown graph " + t.getId());
        }
        return act.apply(g);
    }

    @Override
    public CompletionId thenApply(ThreadId threadID, CompletionId completionId, Serializable code) {
        return withGraph(threadID,
                (graph) -> graph.appendChildNode(completionId,
                        (parent) -> parent.thenApplyNode(serializeClosure(code)))).getId();
    }

    @Override
    public CompletionId whenComplete(ThreadId threadID, CompletionId completionId, Serializable code) {
        return withGraph(threadID,
                (graph) -> graph.appendChildNode(completionId,
                        (parent) -> parent.whenCompleteNode(serializeClosure(code)))).getId();

    }

    @Override
    public CompletionId thenCompose(ThreadId threadId, CompletionId completionId, Serializable code) {
        return withGraph(threadId,
                (graph) -> graph.appendChildNode(completionId,
                        (parent) -> parent.thenComposeNode(serializeClosure(code)))).getId();
    }

    @Override
    public Object waitForCompletion(ThreadId threadId, CompletionId completionId) {
        Graph graph = Objects.requireNonNull(graphs.get(threadId), "Unknown graph");
        Graph.Node node = graph.findNode(completionId).orElseThrow(() -> new RuntimeException("Unknown graph id"));

        try {
            // TODO proper handling
            return node.outputFuture().toCompletableFuture().get().toJavaObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletionId thenAccept(ThreadId threadId, CompletionId completionId, Serializable code) {
        return withGraph(threadId,
                (graph) -> graph.appendChildNode(completionId,
                        (parent) -> parent.thenApplyNode(serializeClosure(code)))).getId();
    }

    @Override
    public CompletionId thenRun(ThreadId threadId, CompletionId completionId, Serializable code) {
        return withGraph(threadId,
                (graph) -> graph.appendChildNode(completionId,
                        (parent) -> parent.thenRunNode(serializeClosure(code)))).getId();

    }

    @Override
    public CompletionId acceptEither(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable code) {
        return withGraph(threadId,
                (graph) ->
                        graph.withNode(alternate,
                                (other) ->
                                        graph.appendChildNode(completionId,
                                                (parent) -> parent.acceptEitherNode(other, serializeClosure(code))).getId()));

    }

    @Override
    public CompletionId applyToEither(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable code) {
        return withGraph(threadId,
                (graph) ->
                        graph.withNode(alternate,
                                (other) ->
                                        graph.appendChildNode(completionId,
                                                (parent) -> parent.applyToEitherNode(other, serializeClosure(code))).getId()));
    }

    @Override
    public CompletionId anyOf(ThreadId threadId, List<CompletionId> cids) {
        return withGraph(threadId,
                (graph) ->
                        graph.withNodes(cids,
                                (nodes) -> {
                                    Graph.Node n = graph.allOf(nodes);
                                    graph.addRootNode(n);
                                    return n.getId();
                                }));

    }

    @Override
    public CompletionId delay(ThreadId threadId, long l) {
        return null;
    }

    @Override
    public CompletionId thenAcceptBoth(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable fn) {
        return null;
    }

    @Override
    public ExternalCompletion createExternalCompletion(ThreadId threadId) {
        return null;
    }

    @Override
    public CompletionId invokeFunction(ThreadId threadId, String functionId, byte[] data, HttpMethod method, Headers headers) {
        return null;
    }

    @Override
    public CompletionId completedValue(ThreadId threadId, Serializable value) {
        return null;
    }

    @Override
    public CompletionId allOf(ThreadId threadId, List<CompletionId> cids) {
        return null;
    }

    @Override
    public CompletionId handle(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return null;
    }

    @Override
    public CompletionId exceptionally(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return null;
    }

    @Override
    public CompletionId thenCombine(ThreadId threadId, CompletionId completionId, Serializable fn, CompletionId alternate) {
        return null;
    }

    @Override
    public void commit(ThreadId threadId) {

    }


    class Graph {
        private final ThreadId graphId;
        private final String functionId;
        private final AtomicInteger nodeCount = new AtomicInteger();
        private final Map<CompletionId, Node> nodes = new ConcurrentHashMap<>();


        Graph(ThreadId graphId, String functionId) {
            this.graphId = graphId;
            this.functionId = functionId;
        }

        Optional<Node> findNode(CompletionId ref) {
            return Optional.ofNullable(nodes.get(ref));
        }

        CompletionId newNodeId() {
            return TestSupport.completinoId("" + nodeCount.incrementAndGet());
        }

        public String getFunctionId() {
            return functionId;
        }

        private void addRootNode(Node node) {
            nodes.put(node.getId(), node);
        }

        private Node appendChildNode(CompletionId cid, Function<Node, Node> ctor) {
            Node newNode = withNode(cid, ctor);
            nodes.put(newNode.getId(), newNode);
            return newNode;
        }

        private <T> T withNode(CompletionId cid, Function<Node, T> function) {
            Node node = nodes.get(cid);
            if (node == null) {
                throw new RuntimeException("Node not  found in graph :" + cid);
            }
            return function.apply(node);
        }

        private <T> T withNodes(List<CompletionId> cids, Function<List<Node>, T> function) {
            List<Node> nodes = new ArrayList<>();
            for (CompletionId cid : cids) {
                Node node = this.nodes.get(cid);
                if (node == null) {
                    throw new RuntimeException("Node not  found in graph :" + cid);
                }
                nodes.add(node);
            }
            return function.apply(nodes);
        }

        private Graph.Node completedValue(Datum value) {
            CompletableFuture<Result> future = CompletableFuture.completedFuture(Result.success(value));
            return new Graph.Node(CompletableFuture.completedFuture(Collections.emptyList()),
                    (x, f) -> future, false);
        }

        private Graph.Node allOf(List<Graph.Node> cns) {
            List<CompletionStage<Result>> outputs = cns.stream().map(Graph.Node::outputFuture).map(CompletionStage::toCompletableFuture).collect(Collectors.toList());

            CompletionStage<Result> output = CompletableFuture
                    .allOf(outputs.toArray(new CompletableFuture<?>[outputs.size()]))
                    .thenApply((nv) -> Result.success(new Datum.EmptyDatum()));

            return new Graph.Node(
                    CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, f) -> output, false);

        }

        private Graph.Node anyOf(List<Graph.Node> cns) {
            List<CompletionStage<Result>> outputs = cns.stream().map(Graph.Node::outputFuture).map(CompletionStage::toCompletableFuture).collect(Collectors.toList());

            CompletionStage<Result> output = CompletableFuture
                    .anyOf(outputs.toArray(new CompletableFuture<?>[outputs.size()])).thenApply((s) -> (Result) s);

            return new Graph.Node(CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, x) -> output,
                    false, cns.toArray(new Graph.Node[cns.size()]));
        }

        private Node supply(Datum.Blob closure) {
            CompletableFuture<List<Result>> input = CompletableFuture.completedFuture(Collections.emptyList());
            return new Node(input, chainInvocation(closure), false);
        }


        private Node externalNode() {
            CompletableFuture<Result> inputFuture = new CompletableFuture<>();
            return new Node(CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, v) -> inputFuture, true);
        }

        private Node delayNode(long delay) {
            CompletableFuture<Result> future = new CompletableFuture<>();
            spe.schedule(() -> future.complete(Result.success(new Datum.EmptyDatum())), delay, TimeUnit.MILLISECONDS);
            return new Node(CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, v) -> future, false);
        }

        private Node invokeFunction(String functionId, HttpMethod method, Headers headers, Datum.Blob data) {
            return new Node(CompletableFuture.completedFuture(Collections.emptyList()),
                    (n, in) -> in.thenComposeAsync((ignored) ->
                            client.invokeFunction(functionId, method, headers, data)), false);
        }

        private BiFunction<Node, CompletionStage<List<Result>>, CompletionStage<Result>> chainInvocation(Datum.Blob closure) {
            return (node, trigger) -> trigger.thenComposeAsync((input) ->
                    client.invokeStage(functionId, node.id, closure, input));
        }

        private final class Node {
            private final CompletionId id;
            private final CompletionStage<List<Result>> inputFuture;
            private final CompletionStage<Result> outputFuture;
            private final boolean externallyCompletable;
            private final List<Node> dependencies;
            private final String created = String.valueOf(System.currentTimeMillis());


            private Node(CompletionStage<List<Result>> input,
                         BiFunction<Node, CompletionStage<List<Result>>, CompletionStage<Result>> invoke,
                         boolean externallyCompletable,
                         Node... dependencies) {

                this.id = newNodeId();
                this.inputFuture = input;
                this.externallyCompletable = externallyCompletable;
                this.dependencies = Collections.unmodifiableList(Arrays.asList(dependencies));
                this.outputFuture = invoke.apply(this, input);
            }


            public CompletionStage<Result> outputFuture() {
                return outputFuture;
            }

            public CompletionId getId() {
                return id;
            }

            private Node thenApplyNode(Datum.Blob closure) {
                return new Node(
                        outputFuture().thenApply(Collections::singletonList),
                        chainInvocation(closure),
                        false, this);
            }

            private Node thenAcceptNode(Datum.Blob closure) {
                return new Node(
                        outputFuture().thenApply(Collections::singletonList),
                        chainInvocation(closure),
                        false, this);
            }

            private Node thenRunNode(Datum.Blob closure) {
                return new Node(
                        outputFuture().thenApply(Collections::singletonList),
                        chainInvocation(closure),
                        false, this);
            }


            private Node thenComposeNode(Datum.Blob closure) {
                BiFunction<Node, CompletionStage<List<Result>>, CompletionStage<Result>> invokefn =
                        chainInvocation(closure)
                                .andThen((resultStage) -> resultStage.thenCompose(result -> {
                                    if (result.getDatum() instanceof Datum.StageRefDatum) {
                                        String ref = ((Datum.StageRefDatum) result.getDatum()).getStageId();

                                        Node node = findNode(TestSupport.completinoId(ref)).orElseThrow(() ->
                                                new ResultException(new Datum.ErrorDatum(Datum.ErrorType.invalid_stage_response, "returned stage not found")));
                                        return node.outputFuture;
                                    } else {
                                        throw new ResultException(new Datum.ErrorDatum(Datum.ErrorType.invalid_stage_response, "Result was not a stageref datum"));
                                    }
                                }));

                return new Node(outputFuture().thenApply(Collections::singletonList), invokefn
                        , false, this);
            }


            public List<Result> resultOrError(Result input, Throwable err) {
                if (err != null) {
                    return Arrays.asList(Result.success(new Datum.EmptyDatum()), errorToResult(err));
                } else {
                    return Arrays.asList(input, Result.success(new Datum.EmptyDatum()));
                }
            }

            private Result errorToResult(Throwable err) {
                if (err instanceof ResultException) {
                    return ((ResultException) err).toResult();
                } else {
                    System.err.println("Unexpected error " + err.toString());
                    err.printStackTrace();
                    return Result.failure(new Datum.ErrorDatum(Datum.ErrorType.unknown_error, "Unexpected error" + err.getMessage()));
                }
            }

            private Node whenCompleteNode(Datum.Blob closure) {
                return new Node(outputFuture().handle(this::resultOrError)
                        , chainInvocation(closure).andThen(c -> outputFuture())
                        , false, this);
            }

            private Node acceptEitherNode(Node otherNode, Datum.Blob closure) {
                return new Node(
                        outputFuture().applyToEither(otherNode.outputFuture, Function.identity())
                                .thenApply(Collections::singletonList),
                        chainInvocation(closure)
                                .andThen(c -> c.thenApply(Result::toEmpty)),
                        false, this, otherNode);
            }

            public Node applyToEitherNode(Node otherNode, Datum.Blob closure) {
                return new Node(
                        outputFuture().applyToEither(otherNode.outputFuture, Collections::singletonList),
                        chainInvocation(closure),
                        false, this, otherNode);
            }

            public Node thenAcceptBothNode(CompletionId completionId, FaaSInvokeClient invokeClient, String functionId, Node otherNode, Datum.Blob closure) {
                return new Node(outputFuture()
                        .thenCombine(otherNode.outputFuture,
                                (input1, input2) -> Arrays.asList(input1, input2)),
                        chainInvocation(closure), false,
                        this, otherNode);
            }

            public boolean isExternallyCompletable() {
                return externallyCompletable;
            }

        }
    }
}
