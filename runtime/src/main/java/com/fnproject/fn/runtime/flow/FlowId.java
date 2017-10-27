package com.fnproject.fn.runtime.flow;

import java.io.Serializable;
import java.util.Objects;

/**
 * Flow Identifier
 * <p>
 * This may be serialized within the runtime class
 */
public final class FlowId implements Serializable {
    private final String id;

    public FlowId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowId flowId = (FlowId) o;
        return Objects.equals(id, flowId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "flow." + getId();
    }
}
