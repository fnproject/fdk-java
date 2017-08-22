package com.fnproject.fn.runtime.cloudthreads;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Holder for captured references to CompletionIds serialized via java serialization
 */
class RefCaptor {

    private static ThreadLocal<Set<CompletionId>> captures = new ThreadLocal<>();


    static void capture(CompletionId cid) {
        Set<CompletionId> capturedSet;
        if ((capturedSet = captures.get()) != null) {
            capturedSet.add(cid);
        }
    }

    static String[] serialize(ObjectOutputStream oos, Object value) throws IOException {
        if (captures.get() != null) {
            throw new IllegalStateException("recursive serialization??");
        }
        captures.set(new HashSet<>());
        try {
            oos.writeObject(value);
            return captures.get().stream().map(CompletionId::getId).toArray(String[]::new);
        } finally {
            captures.set(null);
        }
    }

}
