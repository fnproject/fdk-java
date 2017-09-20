package com.example.fn;

import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Retry {
    public static class RetrySettings implements Serializable {
        public long delayBaseDuration = 1;
        public long delayMaxDuration = 30;
        public TimeUnit timeUnit = TimeUnit.SECONDS;
        public int maxAttempts = 10;
    }

    private static <T extends Serializable> FlowFuture<T> _exponentialWithJitter(Flows.SerCallable<T> op, RetrySettings settings, int attempt) {
        try {
            T result = op.call();
            return Flows.currentFlow().completedValue(result);
        } catch (Exception e) {
            if (attempt < settings.maxAttempts) {
                long delay_max = (long) Math.min(
                        settings.timeUnit.toMillis(settings.delayMaxDuration),
                        settings.timeUnit.toMillis(settings.delayBaseDuration) * Math.pow(2, attempt));
                long delay = new Random().longs(1, 0, delay_max).findFirst().getAsLong();
                return Flows.currentFlow().delay(delay, TimeUnit.MILLISECONDS)
                        .thenCompose((ignored) -> _exponentialWithJitter(op, settings, attempt + 1));
            } else {
                throw new RuntimeException("gave up");
            }
        }
    }

    public static <T extends Serializable> FlowFuture<T>  exponentialWithJitter(Flows.SerCallable<T> op) {
        return _exponentialWithJitter(op, new RetrySettings(), 0);
    }
/*
    public static <T extends Serializable, V extends Serializable> FlowFuture<T> exponentialWithJitter(Flows.SerFunction<T, V> op, V inp) {
        return _exponentialWithJitter(op, new RetrySettings(), 0);
    }*/
}