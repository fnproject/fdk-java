package com.fnproject.fn.runtime.flow;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value type for a completion ID
 */
public final class CompletionId implements Serializable {
    private final String id;

    public CompletionId(String id) {
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

    /**
     * @return The string representation of the completion ID
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "#" + getId();
    }

}
