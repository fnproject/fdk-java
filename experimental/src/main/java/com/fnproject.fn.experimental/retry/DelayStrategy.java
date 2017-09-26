package com.fnproject.fn.experimental.retry;

import java.io.Serializable;

/**
 * DelayStrategy provides a way of determining the maximum time to wait before retrying again.
 */
public interface DelayStrategy {
    /**
     * determies the maximum time to wait before retrying again.
     *
     * @param attempt the attempt number.
     * @return the maximum time to wait before retrying.
     */
    public long delayAmount(int attempt);
}
