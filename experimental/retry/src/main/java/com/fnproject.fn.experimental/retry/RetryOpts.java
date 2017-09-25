package com.fnproject.fn.experimental.retry;


import java.io.Serializable;

public class RetryOpts implements Serializable {

    public StopStrategy stopstrat = new maxAttemptStopStrategy();

    public DelayStrategy delaystrat = new ExponentialDelayStrategy();

    //TODO: public JitterStrategy jitterstrat = new FullJitterStrategy();


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
