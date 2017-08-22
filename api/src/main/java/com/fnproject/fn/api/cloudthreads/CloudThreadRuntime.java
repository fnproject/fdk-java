package com.fnproject.fn.api.cloudthreads;

import com.fnproject.fn.api.Headers;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * A reference to the Cloud Thread object attached to the current invocation context.
 * <p>
 * This provides an API that can be used to trigger asynchronous work within a cloud thread.
 * <p>
 * The methods return {@link CloudFuture} objects that act as the root of an asynchronous computation
 * <p>
 *
 * @see CloudFuture for details of how to chain subsequent work onto these initial methods.
 */
public interface CloudThreadRuntime extends Serializable {

    /**
     * Invoke a fn function and yield the result
     * <p>
     * When this function is called, the completer will send a request with the body to the given function ID within
     * the fn and provide a future that can chain on the response of the function.
     * <blockquote><pre>{@code
     *         CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *         rt.invokeFunction("myapp/myfn","input".getBytes())
     *           .thenAccept((result)->{
     *               System.err.println("Result was " + new String(result));
     *           });
     * <p>
     * }</pre></blockquote>
     *
     * @param functionId Function ID of function to tryInvoke - this should have the form APPNAME/FUNCTION_PATH  (e.g. "myapp/path/to/function" )
     * @param method     HTTP method to invoke function
     * @param headers    Headers to add to the HTTP request representing the function invocation
     * @param data       input data to function as a byte array -
     * @return a future which completes normally if the function succeeded and fails if it fails
     */
    // TODO: convert method from string to HttpMethod enum
    CloudFuture<FunctionResponse> invokeFunction(String functionId, HttpMethod method, Headers headers, byte[] data);

    default CloudFuture<FunctionResponse> invokeFunction(String functionId, HttpMethod method, Headers headers) {
        return invokeFunction(functionId, method, headers, new byte[]{});
    }



    /**
     * Invoke an asynchronous task that yields a value
     * <blockquote><pre>{@code
     *         CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *         rt.supply(()->{
     *            int someVal
     *            someVal = ... // some long running computation.
     *            return someVal;
     *         }).thenAccept((val)->{
     *               System.err.println("Result was " + val);
     *         });
     * }</pre></blockquote>
     *
     * @param c   a callable value to tryInvoke via a thread
     * @param <T> the type of the future
     * @return a com.fnproject.fn.api.cloudthreads.CloudFuture  on
     */
    <T> CloudFuture<T> supply(CloudThreads.SerCallable<T> c);


    /**
     * Invoke an asynchronous task that does not yield a value
     * <blockquote><pre>{@code
     *         CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *         rt.supply(()->{
     *            System.err.println("I have run asynchronously");
     *         });
     * <p>
     * }</pre></blockquote>
     *
     * @param runnable a serializable runnable object
     * @return a completable future that yields when the runnable completes
     */
    CloudFuture<Void> supply(CloudThreads.SerRunnable runnable);

    /**
     * Create a future that completes successfully after a specified delay
     * <blockquote><pre>{@code
     *         CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *         rt.delay(5,TimeUnit.Seconds)
     *            .thenAccept((ignored)->{
     *               System.err.println("I have run asynchronously");
     *            });
     * }</pre></blockquote>
     *
     * @param i  amount to delay
     * @param tu time unit
     * @return a completable future that completes when the delay expires
     */
    CloudFuture<Void> delay(long i, TimeUnit tu);

    /**
     * Create a completed future for a specified value.
     * <blockquote><pre>{@code
     *         CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *         rt.delay(5,TimeUnit.Seconds)
     *            .thenCompose((ignored)->{
     *                if(shouldRunFn){
     *                    return rt.invokeAsync("testapp/testfn","input".getBytes()).thenApply(String::new);
     *                }else{
     *                    return rt.completedValue("some value");
     *                }
     *            })
     *            .thenAccept((x)->System.err.println("Result " + x));
     * }</pre></blockquote>
     *
     * @param value a value to assign to the futures
     * @param <T>   the type of the future value
     * @return a completed cloud future based on the specified value
     */
    <T extends Serializable> CloudFuture<T> completedValue(T value);

    /**
     * Create an externally completable future that can be completed successfully or exceptionally by POSTing data to a public URL.
     *
     * @return an external future
     * @see ExternalCloudFuture for details of how to complete external futures.
     */
    ExternalCloudFuture<byte[]> createExternalFuture();

    /**
     * Wait for all a list of tasks to complete
     * <blockquote><pre>{@code
     *         CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *         CloudFuture<Integer> f1 = rt.delay(5, TimeUnit.SECONDS).thenApply((ignored)-> 10);
     *         CloudFuture<String> f2 = rt.delay(3, TimeUnit.SECONDS).thenApply((ignored)-> "Hello");
     *         CloudFuture<Void> f3 = rt.delay(1, TimeUnit.SECONDS);
     *         rt.allOf(f1,f2,f3)
     *            .thenAccept((ignored)->{
     *            System.err.println("all done");
     *         });
     * }</pre></blockquote>
     * The resulting future will complete successfully if all provided futures complete successfully and will complete exception as soon as any of the provided futures do.
     *
     * @param cloudFutures a list of futures to aggregate, must contain at least one future.
     * @return a future that completes when all futures are complete and fails when any one fails
     */
    CloudFuture<Void> allOf(CloudFuture<?>... cloudFutures);


    /**
     * Wait any of a list of tasks to complete
     * <blockquote><pre>{@code
     *         CloudThreadRuntime rt = CloudThreads.currentRuntime();
     *         CloudFuture<Integer> f1 = rt.delay(5, TimeUnit.SECONDS).thenApply((ignored)-> 10);
     *         CloudFuture<String> f2 = rt.delay(3, TimeUnit.SECONDS).thenApply((ignored)-> "Hello");
     *         CloudFuture<Void> f3 = rt.supply(()->throw new RuntimeException("err"));
     *         rt.anyOf(f1,f2,f3)
     *            .thenAccept((ignored)->{
     *            System.err.println("at least one done");
     *         });
     * }</pre></blockquote>
     * The resulting future will complete successfully as soon as any of the provided futures completes successfully  and only completes exceptionally if all provided futures do.
     *
     * @param cloudFutures a list of futures to aggregate, must contain at least one future
     * @return a future that completes when all futures are complete and fails when any one fails
     */
    CloudFuture<Object> anyOf(CloudFuture<?>... cloudFutures);

}
