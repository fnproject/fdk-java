package com.fnproject.fn.experimental.retry;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class ExponentialDelayStrategy implements DelayStrategy, Serializable {
    public long baseWait = 1;
    public TimeUnit timeUnit = TimeUnit.SECONDS;
    public long delayMaxDuration = 10;

    @Override
    public long delayAmount(int attempt) {
        return (long) Math.min(
                timeUnit.toMillis(delayMaxDuration),
                timeUnit.toMillis(baseWait) * Math.pow(2, attempt)
                );
    }
}