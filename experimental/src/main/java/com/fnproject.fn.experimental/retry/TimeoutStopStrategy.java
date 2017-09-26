package com.fnproject.fn.experimental.retry;

import java.io.Serializable;

/**
 * stops if the total time passed while retrying is greater than timeout
 */
public class TimeoutStopStrategy implements Serializable, StopStrategy {
    public long timeout = 10000;

    @Override
    public boolean shouldRetry(Throwable e, int attempt, long timePassed) {
        return timePassed < timeout;
    }
}
