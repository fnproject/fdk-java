package com.fnproject.fn.testing.cloudthreads;


import com.fnproject.fn.runtime.cloudthreads.CompletionId;
import com.fnproject.fn.runtime.cloudthreads.ThreadId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 26/08/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class Graph {
    private final ThreadId graphId;
    private final String functionId;
    private final AtomicInteger nodeCount  = new AtomicInteger();
    private final Map<CompletionId,Node> nodes = new ConcurrentHashMap<>();


    public Graph(ThreadId graphId, String functionId) {
        this.graphId = graphId;
        this.functionId = functionId;
    }

    public Optional<Node> findNode(CompletionId ref) {
        return Optional.ofNullable(nodes.get(ref));
    }

    public CompletionId newNodeId() {
        return new CompletionId("" + nodeCount.incrementAndGet());
    }

    public String getFunctionId() {
        return functionId;
    }

    public void addNode(Node node) {
        nodes.put(node.getId(),node);
    }
}
