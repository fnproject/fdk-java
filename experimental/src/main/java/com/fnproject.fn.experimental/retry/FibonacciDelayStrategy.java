package com.fnproject.fn.experimental.retry;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * provides Fibonacci backoff delay.
 */
public class FibonacciDelayStrategy implements DelayStrategy, Serializable {

    public long baseWait = 1;
    public TimeUnit timeUnit = TimeUnit.SECONDS;
    public long delayMaxDuration = 10;

    @Override
    public long delayAmount(int attempt) {
        return  Math.min(
                timeUnit.toMillis(delayMaxDuration),
                timeUnit.toMillis(baseWait) * fibonacci(attempt)
        );

    }

    private int fibonacci(int n) {
        //TODO: this is semi-naive and can be improved to O(log(n))
        if (n < 2) {
            return Math.max(0, n);
        }
        int fib1 = 1;
        int fib2 = 1;
        int temp;
        for (int i = 2; i < n; i++) {
            temp = fib2;
            fib2 = fib1 + temp;
            fib1 = temp;
        }
        return fib2;
    }

    public FibonacciDelayStrategy(long baseWait, TimeUnit timeUnit, long delayMaxDuration) {
        this.baseWait = baseWait;
        this.timeUnit = timeUnit;
        this.delayMaxDuration = delayMaxDuration;
    }

    public FibonacciDelayStrategy() {}
}
