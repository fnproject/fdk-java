package com.fnproject.fn.experimental.retry;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class maxAttemptStopStrategy implements StopStrategy, Serializable {
    public int maxAttempts = 5;

    @Override
    public boolean shouldRetry(Throwable e, int attempt, int time, TimeUnit tu) {
        return maxAttempts > attempt;
    }

    public maxAttemptStopStrategy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public maxAttemptStopStrategy() {}
}
