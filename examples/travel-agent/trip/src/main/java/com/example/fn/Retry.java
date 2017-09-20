package com.example.fn;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Retry {

    public static <T> FlowFuture<T> exponentialWithJitter(Flows.SerCallable<FlowFuture<T>> op) {
        return _exponentialWithJitter(op, new RetrySettings(), 1);
    }

    private static <T> FlowFuture<T> _exponentialWithJitter(Flows.SerCallable<FlowFuture<T>> op, RetrySettings settings, int attempt) {
        Flow f = Flows.currentFlow();
        try {
            FlowFuture<T> future = op.call();
            return future.exceptionallyCompose((e) -> {
                if (attempt < settings.maxAttempts) {

                    long delay_max = (long) Math.min(
                        settings.timeUnit.toMillis(settings.delayMaxDuration),
                        settings.timeUnit.toMillis(settings.delayBaseDuration) * Math.pow(2, attempt));
                    long delay = new Random().longs(1, 0, delay_max).findFirst().getAsLong();

                    return f.delay(delay, TimeUnit.MILLISECONDS)
                            .thenCompose((a) -> _exponentialWithJitter(op, settings, attempt + 1));
                } else {
                    return f.failedFuture(new RuntimeException());
                }
            });
        } catch (Exception ex) {
            return f.failedFuture(new RuntimeException());
        }
    }

    static <T> FlowFuture<T> retryThenFail(Flows.SerCallable<FlowFuture<T>> future) {
        return exponentialWithJitter(future)
                .thenCompose((j) -> Flows.currentFlow().failedFuture(new RuntimeException()));
    }

    public static class RetrySettings implements Serializable {
        public long delayBaseDuration = 1;
        public long delayMaxDuration = 10;
        public TimeUnit timeUnit = TimeUnit.SECONDS;
        public int maxAttempts = 5;
    }
}