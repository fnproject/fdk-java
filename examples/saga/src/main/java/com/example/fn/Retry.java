package com.example.fn;

import com.fnproject.fn.api.cloudthreads.CloudFuture;
import com.fnproject.fn.api.cloudthreads.CloudThreads;

import java.io.Serializable;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class Retry {
    public static class RetrySettings implements Serializable {
        public long delayBaseDuration = 1;
        public long delayMaxDuration = 30;
        public TimeUnit timeUnit = TimeUnit.SECONDS;
        public int maxAttempts = 10;
    }

    private static <T extends Serializable> CloudFuture<T> _exponentialWithJitter(CloudThreads.SerCallable<T> op, RetrySettings settings, int attempt) {

        try {
            T result = op.call();
            return CloudThreads.currentRuntime().completedValue(result);
        } catch (Exception e) {
            if (attempt < settings.maxAttempts) {
                long delay_max = (long) Math.min(
                        settings.timeUnit.toMillis(settings.delayMaxDuration),
                        settings.timeUnit.toMillis(settings.delayBaseDuration) * Math.pow(2, attempt));
                long delay = new Random().longs(1, 0, delay_max).findFirst().getAsLong();
                return CloudThreads.currentRuntime().delay(delay, TimeUnit.MILLISECONDS)
                        .thenCompose((ignored) -> _exponentialWithJitter(op, settings, attempt + 1));
            } else {
                throw new RuntimeException("gave up");
            }
        }
    }

    public static <T extends Serializable> CloudFuture<T>  exponentialWithJitter(CloudThreads.SerCallable<T> op, RetrySettings settings) {
        return _exponentialWithJitter(op, settings, 0);
    }

    public static <T extends Serializable> CloudFuture<T>  exponentialWithJitter(CloudThreads.SerCallable<T> op) {
        return _exponentialWithJitter(op, new RetrySettings(), 0);
    }

    public static <T extends Serializable> CloudFuture<T>  exponentialWithJitter(CloudThreads.SerCallable<T> op, TimeUnit timeUnit, long delayBaseDuration, long delayMaxDuration, int maxAttempts) {
        RetrySettings settings = new RetrySettings();
        settings.delayBaseDuration = delayBaseDuration;
        settings.delayMaxDuration  = delayMaxDuration;
        settings.timeUnit = timeUnit;
        settings.maxAttempts = maxAttempts;
        return _exponentialWithJitter(op, new RetrySettings(), 0);
    }
}
