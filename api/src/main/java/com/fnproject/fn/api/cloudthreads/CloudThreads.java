package com.fnproject.fn.api.cloudthreads;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.*;

/**
 * Cloud Threads  API entry point class  - this provides access to the current {@link CloudThreadRuntime} for the current function invocation.
 */
public class CloudThreads {

    private static RuntimeSource runtimeSource;

    /**
     * Gets the current supplier of the cloud threads runtime
     *
     * @return the current supplier of the cloud threads runtime
     */
    public static RuntimeSource getCurrentRuntimeSource() {
        return runtimeSource;
    }

    /**
     * Defines a  supplier for the current runtime-
     */
    public interface RuntimeSource {
        CloudThreadRuntime currentRuntime();
    }

    /**
     * Return the current cloud threads runtime - this will create a new cloud thread if the current is not already bound to the thread or
     * an existing thread if one is already bound to the current invocation or if the invocation was triggered from within a cloud thread.
     *
     * @return the current cloud thread runtime
     */
    public synchronized static CloudThreadRuntime currentRuntime() {
        Objects.requireNonNull(runtimeSource, "CloudThreads.runtimeSource is not set  - CloudThreads.currentRuntime() should only be called from within a FaaS function invocation");
        return runtimeSource.currentRuntime();
    }

    /**
     * Set the current runtime source  - this determines how {@link #currentRuntime()} behaves
     * <p>
     * This is provided for testing and internal use - users should not normally need to modify the runtime source.
     *
     * @param runtimeSource a function that instantiates a runtime on demand
     */
    public synchronized static void setCurrentRuntimeSource(RuntimeSource runtimeSource) {
        CloudThreads.runtimeSource = runtimeSource;
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
     * Version of {@link SerFunction} that signals an exception handler
     */
    @FunctionalInterface
    public interface SerExFunction<T, V> extends SerFunction<T, V>, Serializable {}

    /**
     * Version of {@link BiFunction} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerBiFunction<T, U, R> extends BiFunction<T, U, R>, Serializable {
    }

    /**
     * Version of {@link SerBiFunction} that signals it handles exceptional cases.
     */
    @FunctionalInterface
    public interface SerExBiFunction<T, U, R> extends SerBiFunction<T, U, R>, Serializable {
    }

    /**
     * Version of {@link BiConsumer} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerBiConsumer<T, U> extends BiConsumer<T, U>, Serializable {
    }

    /**
     * Version of {@link SerBiConsumer} that signals it handles exceptional cases.
     */
    @FunctionalInterface
    public interface SerExBiConsumer<T, U> extends SerBiConsumer<T, U>, Serializable {
    }

    /**
     * Version of {@link Runnable} that is implicitly serializable.
     */
    @FunctionalInterface
    public interface SerRunnable extends Runnable, Serializable {
    }


}
