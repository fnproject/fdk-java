package com.fnproject.fn.api.flow;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class exposes operations (with a similar API to {@link java.util.concurrent.CompletionStage}) that allow
 * asynchronous computation to be chained into the current execution graph of a Flow.
 * <p>
 * All asynchronous operations start with a call to an operation on {@link Flow}  - for instance to create a simple asynchronous step call :
 * <blockquote><pre>{@code
 * Flow fl = Flows.currentFlow();
 * int var = 100;
 * // Create a deferred asynchronous computation
 * FlowFuture<Integer>  intFuture = fl.supply(() -> { return 10 * var ;});
 * // Chain future computation onto the completion of the previous computation
 * FlowFuture<String>  stringFuture = intFuture.thenApply(String::valueOf);
 * }</pre></blockquote>
 * <p>
 * FlowFutures are non-blocking by default - once a chained computation has been added to the current flow it will run independently of the calling thread once any
 * dependent FlowFutures are complete (or immediately if it has no dependencies or dependent stages are already completed).
 * <p>
 * As computation is executed remotely (in the form of a captured lambda passed as an argument to a FlowFuture method), the <em>captured context</em> of each of the
 * chained lambdas must be serializable. This includes any captured variables passed into the lambda.
 * <p>
 * For instance, in the above example, the variable {@code x} will be serialized and copied into the execution of the captured lambda.
 * <p>
 * Generally, if a FlowFuture completes exceptionally, it will propagate its exception to any chained dependencies, however {@link #handle(Flows.SerBiFunction)},
 * {@link #whenComplete(Flows.SerBiConsumer)} and {@link #exceptionally(Flows.SerFunction)} allow errors to be caught and handled and
 * {@link #applyToEither(FlowFuture, Flows.SerFunction)} and {@link #acceptEither(FlowFuture, Flows.SerConsumer)} will only propagate errors if all
 * dependent futures fail with an error.
 *
 * @param <T> the type of this future.  All concrete instances of this type must be serializable
 */
public interface FlowFuture<T> extends Serializable {

    /**
     * Applies a transformation to the successfully completed value of this future.
     * <p>
     * This method allows simple sequential chaining of functions together - each stage may be executed independently
     * in a different environment.
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         fl.supply(() -> {
     *         })
     * }</pre></blockquote>
     *
     * @param fn  a serializable function to perform once this future is complete
     * @param <X> the returning type of the function
     * @return a new future which will complete with the value of this future transformed by {@code fn}
     * @see java.util.concurrent.CompletionStage#thenApply(java.util.function.Function)
     */
    <X> FlowFuture<X> thenApply(Flows.SerFunction<T, X> fn);

    /**
     * Compose a new computation on to the completion of this future
     * <p>
     * {@code thenCompose} allows you to dynamically compose one or more steps onto a computation dependent on the value
     * of a previous step while treating the result of those steps as a single future.
     * <p>
     * For instance you may create tail-recursive loops or retrying operations with this call:
     * <blockquote><pre>{@code
     *    private static FlowFuture<String> doSomethingWithRetry(String input, int attemptsLeft) {
     *         Flow fl = Flows.currentFlow();
     *         try{
     *             // some computation that may thrown an error
     *             String result = someUnreliableComputation(input);
     *             return fl.completedValue(result);
     *         }catch(Exception e){
     *             if(attemptsLeft > 0){
     *                  // delay and retry the computation later.
     *                 return rt.delay(5000)
     *                          .thenCompose((ignored) -> doSomethingWithRetry(input, attemptsLeft - 1));
     *             }else{
     *                throw new RuntimeException("Could not do unreliable computation", e);
     *             }
     *         }
     *    }
     * }</pre></blockquote>
     *
     * If the lambda returns null then the compose stage will fail with a {@link InvalidStageResponseException}.
     *
     * @param fn  a serializable function that returns a new computation to chain after the completion of this future
     * @param <X> the type of the future that the composed operation returns
     * @return a new future which will complete in the same way as the future returned by {@code fn}
     * @see java.util.concurrent.CompletionStage#thenCompose(java.util.function.Function)
     */
    <X> FlowFuture<X> thenCompose(Flows.SerFunction<T, FlowFuture<X>> fn);


    /**
     * Compose a new computation on to the completion of this future to handle errors,
     *
     * <p>
     * {@code exceptionallyCompose} works like a combination of {@link #thenCompose(Flows.SerFunction)} and {@link #exceptionally(Flows.SerFunction)}. This allows an error handler to compose a new computation into the graph in order to recover from errors.
     * <p>
     * For instance you may use this to re-try a call :
     * <blockquote><pre>{@code
     *    private static FlowFuture<String> retryOnError(String input, int attemptsLeft) {
     *         Flow fl = Flows.currentFlow();
     *         fl.invokeFunction("./unreliableFunction")
     *           .exceptionallyCompose((t)->{
     *              if(isRecoverable(t) && attemptsLeft  > 0){
     *                  return retryOnError(input,attemptsLeft -1);
     *              }else{
     *                  return fl.failedFuture(t);
     *              }
     *           });
     *    }
     * }</pre></blockquote>
     *
     * If the lambda returns null then the compose stage will fail with a {@link InvalidStageResponseException}.
     *
     * @param fn  a serializable function that returns a new computation to chain after the completion of this future
     * @return a new future which will complete in the same way as the future returned by {@code fn}
     * @see java.util.concurrent.CompletionStage#thenCompose(java.util.function.Function)
     */
    FlowFuture<T> exceptionallyCompose(Flows.SerFunction<Throwable, FlowFuture<T>> fn);

    /**
     * Combine the result of this future and another future into a single value when both have completed normally:
     * <blockquote><pre>{@code
     *  Flow fl = Flows.currentFlow();
     *  FlowFuture<Integer> f1 = fl.supply(() -> {
     *    int x;
     *    // some complex computation
     *    return x;
     *  });
     *
     *  FlowFuture<String> combinedResult = fl.supply(() -> {
     *   int y;
     *   // some complex computation
     *   return x;
     *  }).thenCombine(f1, (a, b) -> return "Result :"  + a + ":" + b);
     * }</pre></blockquote>
     * <p>
     * In the case that either future completes with an exception (regardless of whether the other computation succeeded)
     * or the handler throws an exception then the returned future will complete exceptionally  .
     *
     * @param other a future to combine with this future
     * @param fn    a function to transform the results of the two futures
     * @param <U>   the type of composed future
     * @param <X>   the return type of the combined value
     * @return a new future  that completes with the result of {@code fn} when both {@code this} and {@code other} have
     *         completed and {@code fn} has been run.
     * @see java.util.concurrent.CompletionStage#thenCombine(CompletionStage, BiFunction)
     */
    <U, X> FlowFuture<X> thenCombine(FlowFuture<? extends U> other, Flows.SerBiFunction<? super T, ? super U, ? extends X> fn);

    /**
     * Perform an action when a computation is complete, regardless of whether it completed successfully or with an error.
     * <p>
     * Tha action takes two parameters - representing either the value of the current future or an error if this or any
     * dependent futures failed.
     * <p>
     * Only one of these two parameters will be set, with the other being null.
     * <blockquote><pre>{@code
     *  Flow fl = Flows.currentFlow();
     *  FlowFuture<Integer> f1 = fl.supply(() -> {
     *     if(System.currentTimeMillis() % 2L == 0L) {
     *       throw new RuntimeException("Error in stage");
     *     }
     *     return 100;
     *  });
     *  f1.whenComplete((val, err) -> {
     *     if(err != null){
     *       // an error occurred in upstream stage;
     *     }else{
     *       // the preceding stage was successful
     *     }
     *  });
     * }</pre></blockquote>
     * <p>
     * {@code whenComplete} is always called when the current stage is complete  and does not change the value of current
     * future - to optionally change the value or to use {@link #handle(Flows.SerBiFunction)}
     *
     * @param fn a handler  to call when this stage completes
     * @return a new future that completes as per this future once the action  has run.
     * @see java.util.concurrent.CompletionStage#whenComplete(BiConsumer)
     */
    FlowFuture<T> whenComplete(Flows.SerBiConsumer<T, Throwable> fn);


    /**
     * If not already completed, completes this future with the given value.
     *
     * <p>
     * An example is where you want to complete a future with a default value
     * if it doesn't complete within a given time.
     * </p>
     * <blockquote><pre>{@code
     *
     * Flow flow = Flows.currentFlow();
     * FlowFuture<Integer> f = flow.supply(() -> {
     *      // some computation
     * });
     *
     * // Complete the future with a default value after a timeout
     * flow.delay(10, TimeUnit.SECONDS)
     *  .thenAccept(v -> f.complete(1));
     * }</pre></blockquote>
     *
     * @param value the value to assign to the future
     * @return true if this invocation caused this future to transition to a completed state, else false
     */
    boolean complete(T value);

    /**
     * If not already completed, completes this future exceptionally with the supplied {@link Throwable}.
     *
     * <p>
     * An example is where you want to complete a future exceptionally with a custom
     * exception if it doesn't complete within a given time.
     * </p>
     * <blockquote><pre>{@code
     *
     * Flow flow = Flows.currentFlow();
     * FlowFuture<Integer> f = flow.supply(() -> {
     *      // some computation
     * });
     * flow.delay(10, TimeUnit.SECONDS)
     *  .thenAccept(v -> f.completeExceptionally(new ComputationTimeoutException()));
     * }</pre></blockquote>
     *
     * @param throwable the @{link Throwable} to complete the future exceptionally with
     * @return true if this invocation caused this future to transition to a completed state, else false
     */
    boolean completeExceptionally(Throwable throwable);

    /**
     * If not already completed, completes this future exceptionally with a @{link java.util.concurrent.CancellationException}
     *
     * <p>
     * An example is where you want to cancel the execution of a future and its
     * dependents if it doesn't complete within a given time.
     * </p>
     * <blockquote><pre>{@code
     *
     * Flow flow = Flows.currentFlow();
     * FlowFuture<Integer> f = flow.supply(() -> {
     *      // some computation
     * }).thenAccept(x -> {
     *      // some action
     * });
     * flow.delay(10, TimeUnit.SECONDS)
     *  .thenAccept(v -> f.cancel());
     * }</pre></blockquote>
     *
     * @return true if this invocation caused this future to transition to a completed state, else false
     */
    boolean cancel();

    /**
     * Perform an action when this future completes successfully.
     * <p>
     * This allows you to consume the successful result of a future without returning a new value to future stages.
     * <blockquote><pre>{@code
     *  Flow fl = Flows.currentFlow();
     *  DataBase db;
     *  FlowFuture<Integer> f1 = fl.supply(() -> {
     *        int var;
     *        // some computation
     *        return var;
     *  });
     *  f1.thenAccept((var) -> {
     *      db.storeValue("result", var);
     *  });
     * }</pre></blockquote>
     * <p>
     * If this or a preceding future completes exceptionally then the resulting future will complete exceptionally
     * without the handler being called.
     *
     * @param fn a handler to call when this future completes successfully
     * @return a new future that completes when {@code this} has completed and {@code fn} has been run.
     * @see java.util.concurrent.CompletionStage#thenAccept(Consumer)
     */
    FlowFuture<Void> thenAccept(Flows.SerConsumer<T> fn);

    /**
     * Run an action when the first of two futures completes successfully.
     * <blockquote><pre>{@code
     *  Flow fl = Flows.currentFlow();
     *  FlowFuture<Integer> f1 = fl.supply(() -> {
     *        int var;
     *        // some long-running computation
     *        return var;
     *  });
     *  FlowFuture<Integer> f2 = fl.supply(() -> {
     *       int var;
     *       // some long-running computation
     *       return var;
     *  });
     *  f1.acceptEither(f2, (val) -> {
     *       DataBase db = .... ;
     *       db.storeValue("result", var);
     *  });
     * }</pre></blockquote>
     * <p>
     * In the case that one future completes exceptionally but the other succeeds then the successful value will be used.
     * <p>
     * When both futures complete exceptionally then the returned future will complete exceptionally with one or other of
     * the exception values (which exception is not defined).
     * <p>
     * To transform the value and return it in the future use {@link #applyToEither(FlowFuture, Flows.SerFunction)}.
     *
     * @param alt a future to combine with this future
     * @param fn  a handler to call when the first of this and {@code alt} completes successfully
     * @return a new future that completes normally when either this or {@code alt} completes successfully or completes
     *         exceptionally when both this and {@code alt} complete exceptionally.
     * @see CompletionStage#acceptEither(CompletionStage, Consumer)
     */
    FlowFuture<Void> acceptEither(FlowFuture<? extends T> alt, Flows.SerConsumer<T> fn);


    /**
     * Transform the outcome of the  first of two futures  to complete successfully.
     * <blockquote><pre>{@code
     *   Flow fl = Flows.currentFlow();
     *   FlowFuture<Integer> f1 = fl.supply(() -> {
     *        int var;
     *        // some long-running computation
     *        return var;
     *   });
     *   FlowFuture<Integer> f2 = fl.supply(() -> {
     *        int var;
     *        // some long-running computation
     *        return var;
     *   });
     *   f1.applyToEither(f2, (val) -> {
     *        return "Completed result: " + val;
     *   });
     * }</pre></blockquote>
     * <p>
     * In the case that one future completes exceptionally but the other succeeds then the successful value will be used.
     * <p>
     * When both futures complete exceptionally then the returned future will complete exceptionally with one or other
     * of the exception values (which exception is not defined).
     * <p>
     * To accept a value without transforming it use {@link #acceptEither(FlowFuture, Flows.SerConsumer)}
     *
     * @param alt a future to combine with this one
     * @param fn  a function to transform the first result of this or {@code alt}  with
     * @param <U> the returned type of the transformation
     * @return a new future that completes normally when either this or {@code alt} has completed and {@code fn } has
     *         been completed successfully.
     * @see CompletionStage#applyToEither(CompletionStage, Function)
     */
    <U> FlowFuture<U> applyToEither(FlowFuture<? extends T> alt, Flows.SerFunction<T, U> fn);


    /**
     * Perform an action when this and another future have both completed normally:
     * <blockquote><pre>{@code
     *  Flow fl = Flows.currentFlow();
     *  FlowFuture<Integer> f1 = fl.supply(() -> {
     *      int x;
     *      // some complex computation
     *      return x;
     *  });
     *  FlowFuture<String> combinedResult = fl.supply(() -> {
     *      int y;
     *      // some complex computation
     *      return x;
     *  }).thenAcceptBoth(f1, (a, b) -> {
     *      Database db = ....;
     *      db.store("Result", a + b);
     *  });
     * }</pre></blockquote>
     * <p>
     * In the case that either future completes with an exception (regardless of whether the other computation succeeded)
     * or the handler throws an exception then the returned future will complete exceptionally.
     *
     * @param other a future to combine with this future
     * @param fn    a consumer that accepts the results of  {@code this} and {@code other} when both have completed
     * @param <U>   the type of the composed future
     * @return a new future  that completes with the result of {@code fn} when both {@code this} and {@code other} have
     * completed and  {@code fn} has been run.
     * @see CompletionStage#thenAcceptBoth(CompletionStage, BiConsumer)
     */
    <U> FlowFuture<Void> thenAcceptBoth(FlowFuture<U> other, Flows.SerBiConsumer<T, U> fn);


    /**
     * Run an action when this future has completed normally.
     * <blockquote><pre>{@code
     *  Flow fl = Flows.currentFlow();
     *  FlowFuture<Integer> f1 = fl.supply(() -> {
     *      int x;
     *      // some complex computation
     *      return x;
     *  });
     *  f1.thenRun(() -> System.err.println("computation completed"));
     * }</pre></blockquote>
     * <p>
     * When this future completes exceptionally then the returned future will complete exceptionally.
     *
     * @param fn a runnable to run when this future has completed normally
     * @return a future that will complete after this future has completed and {@code fn} has been run
     * @see CompletionStage#thenRun(Runnable)
     */
    FlowFuture<Void> thenRun(Flows.SerRunnable fn);


    /**
     * Invoke a handler when a computation is complete, regardless of whether it completed successfully or with an error -
     * optionally transforming the resulting value or error to a new value.
     * <p>
     * Tha action takes two parameters - representing either the value of the current future or an error if this or any
     * dependent futures failed.
     * <p>
     * Only one of these two parameters will be set, with the other being null.
     * <blockquote><pre>{@code
     *  Flow fl = Flows.currentFlow();
     *  FlowFuture<String> f1 = fl.supply(() -> {
     *     if(System.currentTimeMillis() % 2L == 0L) {
     *       throw new RuntimeException("Error in stage");
     *     }
     *     return 100;
     *  }).handle((val, err) -> {
     *      if(err != null){
     *          return "An error occurred in this function";
     *      }else{
     *          return "The result was good :" + val;
     *      }
     *  });
     * }</pre></blockquote>
     * <p>
     * {@code handle} is always called when the current stage is complete  and may change the value of the returned future -
     * if you do not need to change this value use  {@link #whenComplete(Flows.SerBiConsumer)}
     *
     * @param fn a handler  to call when this stage completes successfully or with an error
     * @param <X> The type of the transformed output
     * @return a new future that completes as per this future once the action  has run.
     * @see java.util.concurrent.CompletionStage#handle(BiFunction)
     */
    <X> FlowFuture<X> handle(Flows.SerBiFunction<? super T, Throwable, ? extends X> fn);


    /**
     * Handle exceptional completion of this future and convert exceptions to the original type of this future.
     * <p>
     * When an exception occurs within this future (or in a dependent future) - this method allows you to trap and handle
     * that exception (similarly to a catch block) yeilding a new value to return in place of the error.
     * <p>
     * Unlike {@link #handle(Flows.SerBiFunction)} and {@link #whenComplete(Flows.SerBiConsumer)} the handler
     * function is only called when an exception is raised, otherwise the returned future completes with the same value as
     * this future.
     *
     * <blockquote><pre>{@code
     *  Flow fl = Flows.currentFlow();
     *  FlowFuture<Integer> f1 = fl.supply(() -> {
     *     if(System.currentTimeMillis() % 2L == 0L) {
     *       throw new RuntimeException("Error in stage");
     *     }
     *     return 100;
     *  }).exceptionally((err)->{
     *      return - 1;
     *  });
     * }</pre></blockquote>
     *
     * @param fn a handler to trap errors
     * @return a new future that completes with the original value if this future completes successfully or with the
     *         result of calling {@code fn} on the exception value if it completes exceptionally.
     * @see java.util.concurrent.CompletionStage#exceptionally(Function)
     */
    FlowFuture<T> exceptionally(Flows.SerFunction<Throwable, ? extends T> fn);


    /**
     * Get the result of this future
     * <p>
     * This method blocks until the current future completes successfully or with an error.
     * <p>
     * <em> Warning: </em> This method should be used carefully. Blocking within a fn call (triggered by a HTTP request)
     * will result in the calling function remaining active while the underlying computation completes.
     *
     * @return the completed value of this future.
     * @throws FlowCompletionException when this future fails with an exception;
     */
    T get();


    /**
     * Get the result of this future, indicating not to wait over the specified timeout.
     * <p>
     * This method blocks until either the current future completes or the given timeout period elapses,
     * in which case it throws a TimeoutException.
     * <p>
     * <em> Warning: </em> This method should be used carefully. Blocking within a fn call (triggered by a HTTP request)
     * will result in the calling function remaining active while the underlying computation completes.
     *
     * @param timeout the time period after which this blocking get may time out
     * @param unit the time unit of the timeout argument
     * @return the completed value of this future.
     * @throws FlowCompletionException when this future fails with an exception;
     * @throws TimeoutException if the future did not complete within at least the specified timeout
     */
    T get(long timeout, TimeUnit unit) throws TimeoutException;


    /**
     * Get the result of this future if completed or the provided value if absent.
     * <p>
     * This method returns the result of the current future if it is complete. Otherwise, it returns the given
     * valueIfAbsent.
     * <p>
     *
     * @param valueIfAbsent the value to return if not completed
     * @return the value of this future, if completed, else the given valueIfAbsent
     * @throws FlowCompletionException when this future fails with an exception;
     */
    T getNow(T valueIfAbsent);
}
