package com.fnproject.fn.experimental.retry;

import java.io.Serializable;

public interface DelayStrategy {
    public long delayAmount(int attempt);
}
