package com.fnproject.fn.experimental.retry;


import java.io.Serializable;

public class RetryOpts implements Serializable {

    public StopStrategy stopstrat = new maxAttemptStopStrategy();

    public DelayStrategy delaystrat = new ExponentialDelayStrategy();

    //TODO: public JitterStrategy jitterstrat = new FullJitterStrategy();
}
