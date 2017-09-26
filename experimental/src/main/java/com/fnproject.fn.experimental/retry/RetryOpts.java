package com.fnproject.fn.experimental.retry;


import java.io.Serializable;

/**
 * RetryOpts contains the StopStrategy and DelayStrategy for a given retry.
 * It is opinionated about defaults, to reduce cognitive load for end users.
 */
public class RetryOpts implements Serializable {

    public StopStrategy stopstrat = new MaxAttemptStopStrategy();

    public DelayStrategy delaystrat = new ExponentialDelayStrategy();

    public RetryOpts(StopStrategy stopstrat, DelayStrategy delaystrat) {
        this.stopstrat = stopstrat;
        this.delaystrat = delaystrat;
    }

    public RetryOpts(StopStrategy stopstrat) {
        this.stopstrat = stopstrat;
    }

    public RetryOpts(DelayStrategy delaystrat) {
        this.delaystrat = delaystrat;
    }

    public RetryOpts() {
    }
}
