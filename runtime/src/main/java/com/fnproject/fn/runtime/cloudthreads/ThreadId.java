package com.fnproject.fn.runtime.cloudthreads;

import java.io.Serializable;
import java.util.Objects;

/**
 * Thread Identifier
 * <p>
 * This may be serialized within the runtime class
 */
public final class ThreadId implements Serializable {
    private final String id;

    public ThreadId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadId threadId = (ThreadId) o;
        return Objects.equals(id, threadId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getId() {
        return id;
    }

    public String toString(){
        return "thread." + getId();
    }
}
