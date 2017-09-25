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

    public long getBaseWait() {
        return baseWait;
    }

    public void setBaseWait(long baseWait) {
        this.baseWait = baseWait;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public long getDelayMaxDuration() {
        return delayMaxDuration;
    }

    public void setDelayMaxDuration(long delayMaxDuration) {
        this.delayMaxDuration = delayMaxDuration;
    }
}