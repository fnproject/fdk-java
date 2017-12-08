package com.fnproject.fn.api.flow;

import com.fnproject.fn.api.Headers;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * A reference to the Flow object attached to the current invocation context.
 * <p>
 * This provides an API that can be used to trigger asynchronous work within a flow.
 * <p>
 * The methods return {@link FlowFuture} objects that act as the root of an asynchronous computation
 * <p>
 *
 * @see FlowFuture for details of how to chain subsequent work onto these initial methods.
 */
public interface Flow extends Serializable {

    /**
     * Invoke a fn function and yield the result
     * <p>
     * When this function is called, the flow server will send a request with the body to the given function ID within
     * the fn and provide a future that can chain on the response of the function.
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         fl.invokeFunction("myapp/myfn", HttpMethod.GET, Headers.emptyHeaders(), "input".getBytes())
     *           .thenAccept((result)->{
     *               System.err.println("Result was " + new String(result));
     *           });
     * <p>
     * }</pre></blockquote>
     * <p>
     * Function IDs should be of the form "APPID/path/in/app" (without leading slash) where APPID may either be a named application or ".", indicating the appID of the current (calling) function.
     *
     * @param functionId Function ID of function to invoke - this should have the form APPNAME/FUNCTION_PATH  (e.g. "myapp/path/to/function"  or "./path/to/function").
     * @param method     HTTP method to invoke function
     * @param headers    Headers to add to the HTTP request representing the function invocation
     * @param data       input data to function as a byte array -
     * @return a future which completes normally if the function succeeded and fails if it fails
     */
    FlowFuture<HttpResponse> invokeFunction(String functionId, HttpMethod method, Headers headers, byte[] data);

    /**
     * Invoke a function by ID with headers and  an empty body
     * <p>
     *
     * @param functionId Function ID of function to invoke - this should have the form APPNAME/FUNCTION_PATH  (e.g. "myapp/path/to/function"  or "./path/to/function").
     * @param method     HTTP method to invoke function
     * @param headers    Headers to add to the HTTP request representing the function invocation
     * @return a future which completes normally if the function succeeded and fails if it fails
     * @see #invokeFunction(String, HttpMethod, Headers, byte[])
     */
    default FlowFuture<HttpResponse> invokeFunction(String functionId, HttpMethod method, Headers headers) {
        return invokeFunction(functionId, method, headers, new byte[]{});
    }

    /**
     * Invoke a function by ID  using input and output coercion
     * <p>
     * This currently only maps to JSON via the default JSON mapper in the FDK
     *
     * @param functionId   Function ID of function to invoke - this should have the form APPNAME/FUNCTION_PATH  (e.g. "myapp/path/to/function"  or "./path/to/function").
     * @param input        The input object to send to the function input
     * @param responseType The expected response type of the target function
     * @param <T>          The Response type
     * @param <U>          The Input type of the function
     * @return a flow future that completes with the result of the function, or an error if the function invocation failed
     * @throws IllegalArgumentException if the input cannot be coerced to the callA
     */
    default <T extends Serializable, U> FlowFuture<T> invokeFunction(String functionId, U input, Class<T> responseType) {
        return invokeFunction(functionId, HttpMethod.POST, Headers.emptyHeaders(), input, responseType);
    }

    /**
     * Invoke a function by  ID  using input and output coercion and a specified method and headers
     * <p>
     * This currently only maps to JSON via the default JSON mapper in the FDK
     *
     * @param functionId   Function ID of function to invoke - this should have the form APPNAME/FUNCTION_PATH  (e.g. "myapp/path/to/function"  or "./path/to/function").
     * @param method       the HTTP method to use for this call
     * @param headers      additional HTTP headers to pass to this function -
     * @param input        The input object to send to the function input
     * @param responseType The expected response type of the target function
     * @param <T>          The Response type
     * @param <U>          The Input type of the function
     * @return a flow future that completes with the result of the function, or an error if the function invocation failed
     * @throws IllegalArgumentException if the input cannot be coerced to the call
     */
    <T extends Serializable, U> FlowFuture<T> invokeFunction(String functionId, HttpMethod method, Headers headers, U input, Class<T> responseType);


    /**
     * Invoke a function by  ID  using input and output coercion, default method (POST) and  no response type
     * <p>
     * Returns a future that completes with the HttpResponse of the function on success
     * if the function returns a successful http response, and completes with an {@link FunctionInvocationException} if the function invocation fails with a non-succesful http status
     * <p>
     * This currently only maps to JSON via the default JSON mapper in the FDK
     *
     * @param functionId Function ID of function to invoke - this should have the form APPNAME/FUNCTION_PATH  (e.g. "myapp/path/to/function"  or "./path/to/function").
     * @param input      The input object to send to the function input
     * @param <U>        The Input type of the function
     * @return a flow future that completes with the result of the function, or an error if the function invocation failed
     * @throws IllegalArgumentException if the input cannot be coerced to the call
     */
    default <U> FlowFuture<HttpResponse> invokeFunction(String functionId, U input) {
        return invokeFunction(functionId, HttpMethod.POST, Headers.emptyHeaders(), input);
    }

    /**
     * Invoke a function by  ID  using input and output coercion and a specified method and headers
     * <p>
     * Returns a future that completes with the HttpResponse of the function on success
     * if the function returns a successful http response, and completes with an {@link FunctionInvocationException} if the function invocation fails with a non-succesful http status
     * <p>
     * This currently only maps to JSON via the default JSON mapper in the FDK
     *
     * @param functionId Function ID of function to invoke - this should have the form APPNAME/FUNCTION_PATH  (e.g. "myapp/path/to/function"  or "./path/to/function").
     * @param method     the HTTP method to use for this call
     * @param headers    additional HTTP headers to pass to this function -
     * @param input      The input object to send to the function input
     * @param <U>        The Input type of the function
     * @return a flow future that completes with the result of the function, or an error if the function invocation failed
     * @throws IllegalArgumentException if the input cannot be coerced to the call
     */
    <U> FlowFuture<HttpResponse> invokeFunction(String functionId, HttpMethod method, Headers headers, U input);

    /**
     * Invoke a function by ID with no headers
     * <p>
     *
     * @param functionId Function ID of function to invoke - this should have the form APPNAME/FUNCTION_PATH  (e.g. "myapp/path/to/function"  or "./path/to/function").
     * @param method     HTTP method to invoke function
     * @return a future which completes normally if the function succeeded and fails if it fails
     * @see #invokeFunction(String, HttpMethod, Headers, byte[])
     */
    default FlowFuture<HttpResponse> invokeFunction(String functionId, HttpMethod method) {
        return invokeFunction(functionId, method, Headers.emptyHeaders(), new byte[]{});
    }


    /**
     * Invoke an asynchronous task that yields a value
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         fl.supply(()->{
     *            int someVal
     *            someVal = ... // some long running computation.
     *            return someVal;
     *         }).thenAccept((val)->{
     *               System.err.println("Result was " + val);
     *         });
     * }</pre></blockquote>
     *
     * @param c   a callable value to invoke via a flow
     * @param <T> the type of the future
     * @return a com.fnproject.fn.api.flow.FlowFuture  on
     */
    <T> FlowFuture<T> supply(Flows.SerCallable<T> c);


    /**
     * Invoke an asynchronous task that does not yield a value
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         fl.supply(()->{
     *            System.err.println("I have run asynchronously");
     *         });
     * <p>
     * }</pre></blockquote>
     *
     * @param runnable a serializable runnable object
     * @return a completable future that yields when the runnable completes
     */
    FlowFuture<Void> supply(Flows.SerRunnable runnable);

    /**
     * Create a future that completes successfully after a specified delay
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         fl.delay(5,TimeUnit.Seconds)
     *            .thenAccept((ignored)->{
     *               System.err.println("I have run asynchronously");
     *            });
     * }</pre></blockquote>
     *
     * @param i  amount to delay
     * @param tu time unit
     * @return a completable future that completes when the delay expires
     */
    FlowFuture<Void> delay(long i, TimeUnit tu);

    /**
     * Create a completed future for a specified value.
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         fl.delay(5,TimeUnit.Seconds)
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
     * @return a completed flow future based on the specified value
     */
    <T> FlowFuture<T> completedValue(T value);


    /**
     * Create a completed future that propagates a failed value
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         fl.delay(5,TimeUnit.Seconds)
     *            .thenCompose((ignored)->{
     *                if(shouldRunFn){
     *                    return rt.invokeAsync("testapp/testfn","input".getBytes()).thenApply(String::new);
     *                }else{
     *                    return rt.failedFuture(new RuntimeException("Immediate Failure"));
     *                }
     *            })
     *            .whenComplete((x,t)->{
     *                if (t !=null){
     *                   // Will print "Immediate Failure";
     *                   System.err.println("error in flow", t.getMessage());
     *                }else{
     *                   System.err.println("Success! , x);
     *                }
     *            });
     * }</pre></blockquote>
     *
     * @param ex an exception to publish to the future
     * @param <T> the type of the future
     * @return a future that always completes with the specified exception
     */
    <T> FlowFuture<T> failedFuture(Throwable ex);




    /**
     * Create an uncompleted future
     *
     * @param <T> the type of the future
     * @return a flow future that can only be completed via {@link FlowFuture#complete(Object)} or {@link FlowFuture#completeExceptionally(Throwable)}
     */
    <T> FlowFuture<T> createFlowFuture();

    /**
     * Wait for all a list of tasks to complete
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         FlowFuture<Integer> f1 = fl.delay(5, TimeUnit.SECONDS).thenApply((ignored)-> 10);
     *         FlowFuture<String> f2 = fl.delay(3, TimeUnit.SECONDS).thenApply((ignored)-> "Hello");
     *         FlowFuture<Void> f3 = fl.delay(1, TimeUnit.SECONDS);
     *         fl.allOf(f1,f2,f3)
     *            .thenAccept((ignored)->{
     *            System.err.println("all done");
     *         });
     * }</pre></blockquote>
     * The resulting future will complete successfully if all provided futures complete successfully and will complete exception as soon as any of the provided futures do.
     *
     * @param flowFutures a list of futures to aggregate, must contain at least one future.
     * @return a future that completes when all futures are complete and fails when any one fails
     */
    FlowFuture<Void> allOf(FlowFuture<?>... flowFutures);

    /**
     * Wait for any of a list of tasks to complete
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         FlowFuture<Integer> f1 = fl.delay(5, TimeUnit.SECONDS).thenApply((ignored)-> 10);
     *         FlowFuture<String> f2 = fl.delay(3, TimeUnit.SECONDS).thenApply((ignored)-> "Hello");
     *         FlowFuture<Void> f3 = fl.supply(()->throw new RuntimeException("err"));
     *         fl.anyOf(f1,f2,f3)
     *            .thenAccept((ignored)->{
     *            System.err.println("at least one done");
     *         });
     * }</pre></blockquote>
     * The resulting future will complete successfully as soon as any of the provided futures completes successfully  and only completes exceptionally if all provided futures do.
     *
     * @param flowFutures a list of futures to aggregate, must contain at least one future
     * @return a future that completes when all futures are complete and fails when any one fails
     */
    FlowFuture<Object> anyOf(FlowFuture<?>... flowFutures);

    /**
     * Represents the possible end states of a Flow object, i.e. of the whole collection of tasks in the flow.
     * <ul>
     * <li>UNKNOWN indicates that the state of the flow is unknown or invalid</li>
     * <li>SUCCEEDED indicates that the flow ran to completion (i.e. all stages completed successfully or exceptionally)</li>
     * <li>FAILED indicates that the flow failed during its execution, e.g. due to corrupted internal state</li>
     * <li>CANCELLED indicates that the flow was cancelled by the user</li>
     * <li>KILLED indicates that the flow was killed by the user</li>
     * </ul>
     */
    enum FlowState {
        UNKNOWN,
        SUCCEEDED,
        FAILED,
        CANCELLED,
        KILLED
    }

    /**
     * Adds a termination hook that will be executed upon completion of the whole flow; the provided hook will
     * received input according to how the flow terminated.
     * <p>
     * The framework will make a best effort attempt to execute the termination hooks in LIFO order with respect to when
     * they were added.
     * <blockquote><pre>{@code
     *         Flow fl = Flows.currentFlow();
     *         fl.addTerminationHook( (ignored) -> { System.err.println("Flow terminated"); } )
     *           .addTerminationHook( (endState) -> { System.err.println("End state was " + endState.asText()); } );
     * }</pre></blockquote>
     * This example will first run a stage that prints the end state, and then run a stage that prints 'Flow terminated'.
     *
     * @param hook The code to execute
     * @return This same Flow, so that calls can be chained
     */
    Flow addTerminationHook(Flows.SerConsumer<FlowState> hook);
}
