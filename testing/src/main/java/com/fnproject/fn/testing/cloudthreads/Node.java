package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.HttpMethod;
import com.fnproject.fn.runtime.cloudthreads.CompletionId;
import com.fnproject.fn.testing.FnResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

final class Node {
    private final CompletionId id;
    private final CompletionStage<List<Result>> inputFuture;
    private final CompletionStage<Result> outputFuture;
    private final String functionId;
    private final boolean externallyCompletable;
    private final List<Node> dependencies;
    private final String created = String.valueOf(System.currentTimeMillis());


    private Node(CompletionId id, String functionId, CompletionStage<List<Result>> input,
                 Function<CompletionStage<List<Result>>, CompletionStage<Result>> invoke,
                 boolean externallyCompletable,
                 Node... dependencies) {
        this.id = id;
        this.inputFuture = input;
        this.functionId = functionId;
        this.externallyCompletable = externallyCompletable;
        this.dependencies = Collections.unmodifiableList(Arrays.asList(dependencies));

        this.outputFuture = invoke.apply(input);

    }


    private static Function<CompletionStage<List<Result>>, CompletionStage<Result>> chainInvocation(FaaSInvokeClient client, String fnId, CompletionId stageId, Datum.Blob closure) {
        return (trigger) -> trigger.thenComposeAsync((input) ->
                client.invokeStage(fnId, stageId, closure, input));
    }


    public CompletionStage<Result> outputFuture() {
        return outputFuture;
    }


    public CompletionId getId() {
        return id;
    }


    public static Node supply(CompletionId completionId, FaaSInvokeClient client, String functionId, Datum.Blob closure) {
        CompletableFuture<List<Result>> input = CompletableFuture.completedFuture(Collections.emptyList());

        return new Node(completionId, functionId, input, chainInvocation(client, functionId, completionId, closure), false);
    }

    public Node thenApplyNode(CompletionId completionId, FaaSInvokeClient client, String functionId, Datum.Blob closure) {
        return new Node(completionId, functionId,
                outputFuture().thenApply(Collections::singletonList),
                chainInvocation(client, functionId, completionId, closure), false, this);
    }

    public Node thenAcceptNode(CompletionId completionId, FaaSInvokeClient client, String functionId, Datum.Blob closure) {
        return new Node(completionId, functionId,
                outputFuture().thenApply(Collections::singletonList),
                chainInvocation(client, functionId, completionId, closure),
                false, this);
    }

    public Node thenRunNode(CompletionId completionId, FaaSInvokeClient client, String functionId, Datum.Blob closure) {
        return new Node(completionId, functionId,
                outputFuture().thenApply(Collections::singletonList),
                chainInvocation(client, functionId, completionId, closure), false, this);
    }

    // TODO:  Bad place to keep this
    private static ScheduledThreadPoolExecutor spe = new ScheduledThreadPoolExecutor(1);

    public static Node externalNode(CompletionId completionId, String functionId) {
        CompletableFuture<Result> inputFuture = new CompletableFuture<>();

        return new Node(completionId, functionId, CompletableFuture.completedFuture(Collections.emptyList()),
                (v) -> inputFuture, true);
    }

    public static Node delayNode(CompletionId completionId, String functionId, long delay) {
        CompletableFuture<Result> future = new CompletableFuture<>();

        spe.schedule(() -> future.complete(Result.success(new Datum.EmptyDatum())), delay, TimeUnit.MILLISECONDS);
        return new Node(completionId, functionId, CompletableFuture.completedFuture(Collections.emptyList()), (v) -> future, false);
    }


    public Node thenComposeNode(CompletionId completionId, Graph graph, FaaSInvokeClient invokeClient, String functionId, Datum.Blob closure) {

        Function<CompletionStage<List<Result>>, CompletionStage<Result>> invokefn =
                chainInvocation(invokeClient, functionId, completionId, closure)
                        .andThen((resultStage) -> resultStage.thenCompose(result -> {
                            if (result.getDatum() instanceof Datum.StageRefDatum) {
                                String ref = ((Datum.StageRefDatum) result.getDatum()).getStageId();

                                Node node = graph.findNode(new CompletionId(ref)).orElseThrow(() -> new ResultException(new Datum.ErrorDatum(Datum.ErrorType.invalid_stage_response, "returned stage not found")));
                                return node.outputFuture;
                            } else {
                                throw new ResultException(new Datum.ErrorDatum(Datum.ErrorType.invalid_stage_response, "Result was not a stageref datum"));
                            }
                        }));

        return new Node(completionId, functionId, outputFuture().thenApply(Collections::singletonList), invokefn
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

    public Node whenCompleteNode(CompletionId completionId, FaaSInvokeClient client, String functionId, Datum.Blob closure) {
        return new Node(completionId, functionId, outputFuture().handle(this::resultOrError)
                , chainInvocation(client, functionId, completionId, closure)
                , externallyCompletable, this);
    }

    public Node acceptEitherNode(CompletionId completionId, FaaSInvokeClient invokeClient, String functionId, Node otherNode, Datum.Blob closure) {
        return new Node(completionId, functionId,
                outputFuture().applyToEither(otherNode.outputFuture, Function.identity())
                        .thenApply(Collections::singletonList),
                chainInvocation(invokeClient, functionId, completionId, closure)
                        .andThen(c -> c.thenApply(Result::toEmpty)),
                externallyCompletable, this, otherNode);
    }

    public Node applyToEitherNode(CompletionId completionId, FaaSInvokeClient invokeClient, String functionId, Node otherNode, Datum.Blob closure) {
        return new Node(completionId, functionId,
                outputFuture().applyToEither(otherNode.outputFuture, Function.identity())
                        .thenApply(Collections::singletonList),
                chainInvocation(invokeClient, functionId, completionId, closure),
                externallyCompletable, this, otherNode);
    }

    public Node thenAcceptBothNode(CompletionId completionId, FaaSInvokeClient invokeClient, String functionId, Node otherNode, Datum.Blob closure) {
        return new Node(completionId, functionId, outputFuture()
                .thenCombine(otherNode.outputFuture,
                        (input1, input2) -> Arrays.asList(input1, input2)),
                chainInvocation(invokeClient, functionId, completionId, closure), externallyCompletable, this, otherNode);
    }


    public String getFunctionId() {
        return functionId;
    }

    public boolean isExternallyCompletable() {
        return externallyCompletable;
    }

    public List<Node> getDependencies() {
        return dependencies;
    }

    public static Datum fnResultToDatum(FnResult result) {
        return new Datum.HttpRespDatum(result.getStatus(), result.getHeaders(), result.getBodyAsBytes());
    }

    public static Node invokeFunction(CompletionId completionId, FaaSInvokeClient invokeClient, String functionId, HttpMethod method, Headers headers, Datum.Blob data) {

        return new Node(completionId, functionId, CompletableFuture.completedFuture(Collections.emptyList()),
                (in) -> in.thenComposeAsync((ignored) ->
                        invokeClient.invokeFunction(functionId, method, headers, data)), false);
    }

    public static Node completedValue(String completion, String functionId, Datum value) {
        CompletableFuture<Datum> future = CompletableFuture.completedFuture(value);
        return new Node(completion, functionId, future, (f) -> f, false);
    }

    public static Node allOf(CompletionId completionId, List<Node> cns) {
        List<CompletionStage<Result>> outputs = cns.stream().map(Node::outputFuture).map(CompletionStage::toCompletableFuture).collect(Collectors.toList());


        CompletionStage<Result> output = CompletableFuture.allOf(outputs.toArray(new CompletableFuture<?>[outputs.size()])).thenApply((nv) -> Result.success(new Datum.EmptyDatum()));

        return new Node(completionId, "<none>", CompletableFuture.completedFuture(Collections.emptyList()), (x) -> output, false, cns.toArray(new Node[cns.size()]));
    }

    public static Node anyOf(CompletionId completionId, List<Node> cns) {
        List<CompletionStage<Result>> outputs = cns.stream().map(Node::outputFuture).map(CompletionStage::toCompletableFuture).collect(Collectors.toList());

        CompletionStage<Result> output = CompletableFuture.anyOf(outputs.toArray(new CompletableFuture<?>[outputs.size()])).thenApply((s) -> (Result) s);

        return new Node(completionId, "<none>", CompletableFuture.completedFuture(Collections.emptyList()), (x) -> output, false, cns.toArray(new Node[cns.size()]));
    }

}
