package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.HttpMethod;
import com.fnproject.fn.runtime.cloudthreads.CompleterClient;
import com.fnproject.fn.runtime.cloudthreads.CompletionId;
import com.fnproject.fn.runtime.cloudthreads.ThreadId;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 25/08/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class InMemCompleter implements CompleterClient {
    final Map<ThreadId, Graph> graphs = new ConcurrentHashMap<>();
    final AtomicInteger threadCount = new AtomicInteger();
    final FaaSInvokeClient client ;

    public InMemCompleter(FaaSInvokeClient client) {
        this.client = client;
    }

    @Override
    public ThreadId createThread(String functionId) {
        ThreadId id = new ThreadId("thread-" + threadCount.incrementAndGet());

        graphs.put(id, new Graph(id, functionId));

        return id;
    }

    @Override
    public CompletionId supply(ThreadId threadID, Serializable code) {
        Graph graph = Objects.requireNonNull(graphs.get(threadID),"Unknown graph");
        CompletionId completionId = graph.newNodeId();
        graph.addNode( Node.supply(completionId,client,graph.getFunctionId(),serializeClosure(code) ));
        return completionId;

    }

    private Datum.Blob serializeClosure(Serializable code) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(code);
            oos.close();
            return new Datum.Blob("application/x-java-serialized-object",bos.toByteArray());

        }catch(Exception e){
            throw new RuntimeException("Error serializing closure ",e);
        }
    }

    @Override
    public CompletionId thenApply(ThreadId threadID, CompletionId completionId, Serializable code) {
        Graph graph = Objects.requireNonNull(graphs.get(threadID),"Unknown graph");
        Node node = graph.findNode(completionId).orElseThrow(()->new RuntimeException("Unknown graph id"));


        CompletionId newId = graph.newNodeId();
        graph.addNode(node.thenApplyNode(newId,client,graph.getFunctionId(),serializeClosure(code) ));
        return newId;
    }

    @Override
    public CompletionId whenComplete(ThreadId threadID, CompletionId completionId, Serializable code) {
        Graph graph = Objects.requireNonNull(graphs.get(threadID),"Unknown graph");
        Node node = graph.findNode(completionId).orElseThrow(()->new RuntimeException("Unknown graph id"));


        CompletionId newId = graph.newNodeId();
        graph.addNode(node.whenCompleteNode(newId,client,graph.getFunctionId(),serializeClosure(code) ));
        return newId;
    }

    @Override
    public CompletionId thenCompose(ThreadId threadId, CompletionId completionId, Serializable code) {
        Graph graph = Objects.requireNonNull(graphs.get(threadId),"Unknown graph");
        Node node = graph.findNode(completionId).orElseThrow(()->new RuntimeException("Unknown graph id"));

        CompletionId newId = graph.newNodeId();
        graph.addNode(node.thenComposeNode(newId,graph,client,graph.getFunctionId(),serializeClosure(code) ));
        return newId;
    }

    @Override
    public Object waitForCompletion(ThreadId threadId, CompletionId completionId) {
        Graph graph = Objects.requireNonNull(graphs.get(threadId),"Unknown graph");
        Node node = graph.findNode(completionId).orElseThrow(()->new RuntimeException("Unknown graph id"));

        try {
            node.outputFuture().toCompletableFuture().get().toJavaObject();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletionId thenAccept(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return null;
    }

    @Override
    public CompletionId thenRun(ThreadId threadId, CompletionId completionId, Serializable fn) {
        return null;
    }

    @Override
    public CompletionId acceptEither(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable fn) {
        return null;
    }

    @Override
    public CompletionId applyToEither(ThreadId threadId, CompletionId completionId, CompletionId alternate, Serializable fn) {
        return null;
    }

    @Override
    public CompletionId anyOf(ThreadId threadId, List<CompletionId> cids) {
        return null;
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
}
