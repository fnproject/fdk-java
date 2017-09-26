package com.fnproject.fn.experimental.retry;

import java.io.Serializable;

/**
 * stops after maxAttempts has been reached.
 */
public class MaxAttemptStopStrategy implements StopStrategy, Serializable {
    public int maxAttempts = 5;

    @Override
    public boolean shouldRetry(Throwable e, int attempt, long timePassed) {
        return maxAttempts > attempt;
    }

    public MaxAttemptStopStrategy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public MaxAttemptStopStrategy() {}
}
