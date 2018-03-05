package com.fnproject.fn.api.flow;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.*;

/**
 * Fn Flow API entry point class  - this provides access to the current {@link Flow} for the current function invocation.
 */
public final class Flows {
    private Flows() {
    }

    private static FlowSource flowSource;

    /**
     * Gets the current supplier of the flow runtime
     *
     * @return the current supplier of the flow runtime
     */
    public static FlowSource getCurrentFlowSource() {
        return flowSource;
    }

    /**
     * Defines a  supplier for the current runtime-
     */
    public interface FlowSource {
        Flow currentFlow();
    }

    /**
     * Return the current flow runtime - this will create a new flow if the current is not already bound to the invocation or
     * an existing flow if one is already bound to the current invocation or if the invocation was triggered from within a flow stage.
     *
     * @return the current flow runtime
     */
    public synchronized static Flow currentFlow() {
        Objects.requireNonNull(flowSource, "Flows.flowSource is not set  - Flows.currentFlow() should only be called from within a FaaS function invocation");
        return flowSource.currentFlow();
    }

    /**
     * Set the current runtime source  - this determines how {@link #currentFlow()} behaves
     * <p>
     * This is provided for testing and internal use - users should not normally need to modify the runtime source.
     *
     * @param flowSource a function that instantiates a runtime on demand
     */
    public synchronized static void setCurrentFlowSource(FlowSource flowSource) {
        Flows.flowSource = flowSource;
    }


    /**
     * Version of {@link Callable} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerCallable<V> extends Callable<V>, Serializable {
    }

    /**
     * Version of {@link Supplier} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerSupplier<V> extends Supplier<V>, Serializable {
    }

    /**
     * Version of {@link Consumer} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerConsumer<V> extends Consumer<V>, Serializable {
    }

    /**
     * Version of {@link Function} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerFunction<T, V> extends Function<T, V>, Serializable {
    }

    /**
     * Version of {@link BiFunction} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerBiFunction<T, U, R> extends BiFunction<T, U, R>, Serializable {
    }

    /**
     * Version of {@link BiConsumer} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerBiConsumer<T, U> extends BiConsumer<T, U>, Serializable {
    }

    /**
     * Version of {@link Runnable} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerRunnable extends Runnable, Serializable {
    }


}
