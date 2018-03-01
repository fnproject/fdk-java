package com.fnproject.fn.experimental.retry;

/**
 * StopStrategy provides a way of determining when to stop retrying.
 */
public interface StopStrategy {
    /**
     * shouldRetry determines whether to stop retrying the operation, or continue.
     *
     * @param e the exception thrown by the operation tried, on its previous attempt.
     * @param attempt the number of the attempt.
     * @param timePassed the time passed since the first attempt.
     * @return true if the operation should be retried, false otherwise.
     */
    public boolean shouldRetry(Throwable e, int attempt, long timePassed);
}