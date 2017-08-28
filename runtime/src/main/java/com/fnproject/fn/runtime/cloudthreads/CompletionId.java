package com.fnproject.fn.runtime.cloudthreads;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value type for a completion ID
 */
final class CompletionId implements Serializable {
    private final String id;

    CompletionId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompletionId that = (CompletionId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    protected String getId(){
        return id;
    }


}
