package com.fnproject.fn.experimental.retry;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Retry {

    /**
     * retry is a synonym for retryExponentialWithJitter.
     *
     * @param operation the operation to be retried.
     * @param <T> the type that the FlowFuture produced by operation completes to.
     * @return the FlowFuture that operation returns, or a FailedFuture
     * generated with the exception produced by operation.
     */
    public static <T> FlowFuture<T> retry(Flows.SerCallable<FlowFuture<T>> operation) {
        return retryExponentialWithJitter(operation);
    }

    /**
     * retryFibonacciWithJitter retries the operation with Fibonacci backoff, stopping after 5 attempts.
     * Each individual wait period is limited to a maximum of 10 seconds.
     *
     * @param operation the operation to be retried.
     * @param <T> the type that the FlowFuture produced by operation completes to.
     * @return the FlowFuture that operation returns, or a FailedFuture
     * generated with the exception produced by operation.
     */
    public static <T> FlowFuture<T> retryFibonacciWithJitter(Flows.SerCallable<FlowFuture<T>> operation) {
        return retry(operation, new RetryOpts(new FibonacciDelayStrategy()), 1, 0);
    }

    /**
     * retryExponentialWithJitter retries the operation with exponential backoff, stopping after 5 attempts.
     * Each individual wait period is limited to a maximum of 10 seconds.
     *
     * @param operation the operation to be retried.
     * @param <T> the type that the FlowFuture produced by operation completes to.
     * @return the FlowFuture that operation returns, or a FailedFuture
     * generated with the exception produced by operation.
     */
    public static <T> FlowFuture<T> retryExponentialWithJitter(Flows.SerCallable<FlowFuture<T>> operation) {
        return retry(operation, new RetryOpts(), 1, 0);
    }

    /**
     * retryWithOpts retries the operation with the delay and stop strategies defined in the RetryOpts object provided.
     *
     * @param operation the operation to be retried.
     * @param opts the RetryOpts object that defines the retry strategy to be used.
     * @param <T> the type that the FlowFuture produced by operation completes to.
     * @return the FlowFuture that operation returns, or a FailedFuture
     * generated with the exception produced by operation.
     */
    public static <T> FlowFuture<T> retryWithOpts(Flows.SerCallable<FlowFuture<T>> operation, RetryOpts opts) {
        return retry(operation, opts, 1, 0);
    }

    /**
     * retry is a private and recursive method used by all other retry methods. It actually carries out the retry operations.
     *
     * @param operation the operation to be retried.
     * @param opts the RetryOpts object that defines the retry strategy to be used.
     * @param attempt the number of this attempt.
     * @param timePassed the time in milliseconds that has passed since the first attempt.
     * @param <T> the type that the FlowFuture produced by operation completes to.
     * @return the FlowFuture that operation returns, or a FailedFuture
     * generated with the exception produced by operation.
     */
    private static <T> FlowFuture<T> retry(Flows.SerCallable<FlowFuture<T>> operation, RetryOpts opts, int attempt, long timePassed) {
        Flow f = Flows.currentFlow();
        try {
            FlowFuture<T> future = operation.call();

            return future.exceptionallyCompose((e) -> {

                if (opts.stopstrat.shouldRetry(e, attempt, timePassed)) {
                    long delay = getDelay(opts, attempt);
                    return f.delay(delay, TimeUnit.MILLISECONDS)
                            .thenCompose((a) -> retry(operation, opts, attempt + 1, timePassed + delay));
                } else {
                    return f.failedFuture(e);
                }
            });

        } catch (Exception ex) {
            return f.failedFuture(ex);
        }
    }

    /**
     * getDelay generates the delay amount based on the DelayStrategy in opts.
     *
     * @param opts the RetryOpts object containing the DelayStrategy.
     * @param attempt the attempt number.
     * @return the maximum delay time to wait in milliseconds.
     */
    private static long getDelay(RetryOpts opts, int attempt) {
        long delay_max = opts.delaystrat.delayAmount(attempt);
        return new Random().longs(1, 0, delay_max).findFirst().getAsLong();
    }

}
