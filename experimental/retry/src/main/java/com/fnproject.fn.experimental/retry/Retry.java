package com.fnproject.fn.experimental.retry;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Retry {

    public static <T> FlowFuture<T> retryFibonacciWithJitter(Flows.SerCallable<FlowFuture<T>> operation) {
        return retry(operation, new RetryOpts(new FibonacciDelayStrategy()), 1);
    }

    public static <T> FlowFuture<T> retryExponentialWithJitter(Flows.SerCallable<FlowFuture<T>> operation) {
        return retry(operation, new RetryOpts(), 1);
    }

    public static <T> FlowFuture<T> retryWithOpts(Flows.SerCallable<FlowFuture<T>> operation, RetryOpts opts) {
        return retry(operation, opts, 1);
    }

    private static <T> FlowFuture<T> retry(Flows.SerCallable<FlowFuture<T>> operation, RetryOpts opts, int attempt) {
        Flow f = Flows.currentFlow();
        try {
            FlowFuture<T> future = operation.call();

            return future.exceptionallyCompose((e) -> {

                if (opts.stopstrat.shouldRetry(e, attempt, 0, TimeUnit.SECONDS)) {
                    long delay = getDelay(opts, attempt);
                    return f.delay(delay, TimeUnit.MILLISECONDS)
                            .thenCompose((a) -> retry(operation, opts, attempt + 1));
                } else {
                    return f.failedFuture(e);
                }
            });

        } catch (Exception ex) {
            return f.failedFuture(ex);
        }
    }

    private static long getDelay(RetryOpts opts, int attempt) {
        //TODO: JitterStrategies
        long delay_max = opts.delaystrat.delayAmount(attempt);
        return new Random().longs(1, 0, delay_max).findFirst().getAsLong();
    }

}
