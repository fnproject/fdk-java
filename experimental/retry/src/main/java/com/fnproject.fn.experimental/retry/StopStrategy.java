package com.fnproject.fn.experimental.retry;

import java.util.concurrent.TimeUnit;

public interface StopStrategy {
    public boolean shouldRetry(Throwable e, int attempt, int time, TimeUnit tu);
}