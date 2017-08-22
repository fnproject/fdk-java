package com.fnproject.fn.api.cloudthreads;

import java.io.Serializable;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class exposes operations (with a similar API to {@link java.util.concurrent.CompletionStage}) that allow
 * asynchronous computation to be chained into the current execution graph of a Cloud Thread.
 * <p>
 * All asynchronous operations start with a call to an operation on {@link CloudThreadRuntime}  - for instance to create a simple asynchronous step call :
 * <blockquote><pre>{@code
 * CloudThreadRuntime rt = CloudThreads.currentRuntime();
 * int var = 100;
 * // Create a deferred asynchronous computation
 * CloudFuture<Integer>  intFuture = rt.supply(() -> { return 10 * var ;});
 * // Chain future computation onto the completion of the previous computation
 * CloudFuture<String>  stringFuture = intFuture.thenApply(String::valueOf);
 * }</pre></blockquote>
 * <p>
 * CloudFutures are non-blocking by default - once a chained computation has been added to the current thread it will run independently of the calling thread once any
 * dependent CloudFutures are  complete (or immediately if it has no dependencies or dependent  stages  are  already completed).
 * <p>
 * As computation is executed remotely (in the form of a captured lambda passed as an argument to a CloudFuture method), the <em>captured context</em> of each of the
 * chained lambdas must be serializable. This includes any captured variables passed into the lambda.
 * <p>
 * For instance, in the above example, the variable {@code x} will be serialized and copied into the execution of the captured lambda.
 * <p>
 * Generally, if a CloudFuture completes exceptionally, it will propagate its exception to any chained dependencies, however {@link #handle(CloudThreads.SerBiFunction)},
 * {@link #whenComplete(CloudThreads.SerBiConsumer)} and {@link #exceptionally(CloudThreads.SerFunction)} allow errors to be caught and handled and
 * {@link #applyToEither(CloudFuture, CloudThreads.SerFunction)} and {@link #acceptEither(CloudFuture, CloudThreads.SerConsumer)} will only propagate errors if all
 * dependent futures fail with an error.
 *
 * @param <T> the type of this future.  All concrete instances of this type must be serializable
 */
public interface CloudFuture<T> extends Serializable {

    /**
     * Applies a transformation to the successfully completed value of this future.
     * <p>
     * This method allows simple sequential chaining of functions together - each stage may be executed independently
     * in a different environment.
     * <blockquote><pre>{@code
     *         CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *         rt.supply(() -> {
     *         })
     * }</pre></blockquote>
     *
     * @param fn  a serializable function to perform once this future is complete
     * @param <X> the returning type of the function
     * @return a new future which will complete with the value of this future transformed by {@code fn}
     * @see java.util.concurrent.CompletionStage#thenApply(java.util.function.Function)
     */
    <X> CloudFuture<X> thenApply(CloudThreads.SerFunction<T, X> fn);

    /**
     * Compose a new computation on to the completion of this future
     * <p>
     * {@code thenCompose} allows you to dynamically compose one or more steps onto a computation dependent on the value
     * of a previous step while treating the result of those steps as a single future.
     * <p>
     * For instance you may create tail-recursive loops or retrying operations with this call:
     * <blockquote><pre>{@code
     *    private static CloudFuture<String> doSomethingWithRetry(String input, int attemptsLeft) {
     *         CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *         try{
     *             // some computation that may thrown an error
     *             String result = someUnreliableComputation(input);
     *             return rt.completedValue(result);
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
    <X> CloudFuture<X> thenCompose(CloudThreads.SerFunction<T, CloudFuture<X>> fn);

    /**
     * Combine the result of this future and another future into a single value when both have completed normally:
     * <blockquote><pre>{@code
     *  CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *  CloudFuture<Integer> f1 = rt.supply(() -> {
     *    int x;
     *    // some complex computation
     *    return x;
     *  });
     *
     *  CloudFuture<String> combinedResult = rt.supply(() -> {
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
    <U, X> CloudFuture<X> thenCombine(CloudFuture<? extends U> other, CloudThreads.SerBiFunction<? super T, ? super U, ? extends X> fn);

    /**
     * Perform an action when a computation is complete, regardless of whether it completed successfully or with an error.
     * <p>
     * Tha action takes two parameters - representing either the value of the current future or an error if this or any
     * dependent futures failed.
     * <p>
     * Only one of these two parameters will be set, with the other being null.
     * <blockquote><pre>{@code
     *  CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *  CloudFuture<Integer> f1 = rt.supply(() -> {
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
     * future - to optionally change the value or to use {@link #handle(CloudThreads.SerBiFunction)}
     *
     * @param fn a handler  to call when this stage completes
     * @return a new future that completes as per this future once the action  has run.
     * @see java.util.concurrent.CompletionStage#whenComplete(BiConsumer)
     */
    CloudFuture<T> whenComplete(CloudThreads.SerBiConsumer<T, Throwable> fn);

    /**
     * Perform an action when this future completes successfully.
     * <p>
     * This allows you to consume the successful result of a future without returning a new value to future stages.
     * <blockquote><pre>{@code
     *  CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *  DataBase db;
     *  CloudFuture<Integer> f1 = rt.supply(() -> {
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
    CloudFuture<Void> thenAccept(CloudThreads.SerConsumer<T> fn);

    /**
     * Run an action when the first of two futures completes successfully.
     * <blockquote><pre>{@code
     *  CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *  CloudFuture<Integer> f1 = rt.supply(() -> {
     *        int var;
     *        // some long-running computation
     *        return var;
     *  });
     *  CloudFuture<Integer> f2 = rt.supply(() -> {
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
     * To transform the value and return it in the future use {@link #applyToEither(CloudFuture, CloudThreads.SerFunction)}.
     *
     * @param alt a future to combine with this future
     * @param fn  a handler to call when the first of this and {@code alt} completes successfully
     * @return a new future that completes normally when either this or {@code alt} completes successfully or completes
     *         exceptionally when both this and {@code alt} complete exceptionally.
     * @see CompletionStage#acceptEither(CompletionStage, Consumer)
     */
    CloudFuture<Void> acceptEither(CloudFuture<? extends T> alt, CloudThreads.SerConsumer<T> fn);


    /**
     * Transform the outcome of the  first of two futures  to complete successfully.
     * <blockquote><pre>{@code
     *   CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *   CloudFuture<Integer> f1 = rt.supply(() -> {
     *        int var;
     *        // some long-running computation
     *        return var;
     *   });
     *   CloudFuture<Integer> f2 = rt.supply(() -> {
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
     * To accept a value without transforming it use {@link #acceptEither(CloudFuture, CloudThreads.SerConsumer)}
     *
     * @param alt a future to combine with this one
     * @param fn  a function to transform the first result of this or {@code alt}  with
     * @param <U> the returned type of the transformation
     * @return a new future that completes normally when either this or {@code alt} has completed and {@code fn } has
     *         been completed successfully.
     * @see CompletionStage#applyToEither(CompletionStage, Function)
     */
    <U> CloudFuture<U> applyToEither(CloudFuture<? extends T> alt, CloudThreads.SerFunction<T, U> fn);


    /**
     * Perform an action when this and another future have both completed normally:
     * <blockquote><pre>{@code
     *  CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *  CloudFuture<Integer> f1 = rt.supply(() -> {
     *      int x;
     *      // some complex computation
     *      return x;
     *  });
     *  CloudFuture<String> combinedResult = rt.supply(() -> {
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
    <U> CloudFuture<Void> thenAcceptBoth(CloudFuture<U> other, CloudThreads.SerBiConsumer<T, U> fn);


    /**
     * Run an action when this future has completed normally.
     * <blockquote><pre>{@code
     *  CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *  CloudFuture<Integer> f1 = rt.supply(() -> {
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
    CloudFuture<Void> thenRun(CloudThreads.SerRunnable fn);


    /**
     * Invoke a handler when a computation is complete, regardless of whether it completed successfully or with an error -
     * optionally transforming the resulting value or error to a new value.
     * <p>
     * Tha action takes two parameters - representing either the value of the current future or an error if this or any
     * dependent futures failed.
     * <p>
     * Only one of these two parameters will be set, with the other being null.
     * <blockquote><pre>{@code
     *  CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *  CloudFuture<String> f1 = rt.supply(() -> {
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
     * if you do not need to change this value use  {@link #whenComplete(CloudThreads.SerBiConsumer)}
     *
     * @param fn a handler  to call when this stage completes successfully or with an error
     * @param <X> The type of the transformed output
     * @return a new future that completes as per this future once the action  has run.
     * @see java.util.concurrent.CompletionStage#handle(BiFunction)
     */
    <X> CloudFuture<X> handle(CloudThreads.SerBiFunction<? super T, Throwable, ? extends X> fn);

    /**
     * Handle exceptional completion of this future and convert exceptions to the original type of this future.
     * <p>
     * When an exception occurs within this future (or in a dependent future) - this method allows you to trap and handle
     * that exception (similarly to a catch block) yeilding a new value to return in place of the error.
     * <p>
     * Unlike {@link #handle(CloudThreads.SerBiFunction)} and {@link #whenComplete(CloudThreads.SerBiConsumer)} the handler
     * function is only called when an exception is raised, otherwise the returned future completes with the same value as
     * this future.
     *
     * <blockquote><pre>{@code
     *  CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *  CloudFuture<Integer> f1 = rt.supply(() -> {
     *     if(System.currentTimeMillis() % 2L == 0L) {
     *       throw new RuntimeException("Error in stage");
     *     }
     *     return 100;
     *  }).exceptionally((err)->{
     *      return - 1;
     *  });
     * }</pre></blockquote>
     *
     * @param fn a handler to trap errors .
     * @return a new future that completes with the original value if this future completes successfully or with the
     *         result of calling {@code fn} on the exception value if it completes exceptionally.
     * @see java.util.concurrent.CompletionStage#exceptionally(Function)
     */
    CloudFuture<T> exceptionally(CloudThreads.SerFunction<Throwable, ? extends T> fn);


    /**
     * Get the result of this future
     * <p>
     * This method blocks until the current future completes successfully or with an error.
     * <p>
     * <em> Warning: </em> This method should be used carefully. Blocking within a fn call (triggered by a HTTP request)
     * will result in the calling function remaining active while the underlying computation completes.
     *
     * @return the completed value of this future.
     * @throws CloudCompletionException when this future fails with an exception;
     */
    T get();
}
