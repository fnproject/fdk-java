package com.fnproject.fn.experimental.retry;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class StrategyTests {

    @Test
    public void ExponentialDelayStrategyTest() {
        assertEquals(4000, new ExponentialDelayStrategy().delayAmount(2));
        assertEquals(10000, new ExponentialDelayStrategy().delayAmount(6));
        assertEquals(64000, new ExponentialDelayStrategy(1, TimeUnit.SECONDS, 100).delayAmount(6));
    }

    @Test
    public void FibonacciDelayStrategyTest() {
        assertEquals(5000, new FibonacciDelayStrategy().delayAmount(5));
        assertEquals(10000, new FibonacciDelayStrategy().delayAmount(20));
        assertEquals(6765000, new FibonacciDelayStrategy(1, TimeUnit.SECONDS, 10000).delayAmount(20));
    }

    @Test
    public void MaxAttemptStopStrategyTest() {
        assert(new MaxAttemptStopStrategy().shouldRetry(new Exception(), 3, 10));
        assert(!(new MaxAttemptStopStrategy().shouldRetry(new Exception(), 7, 10)));
        assert(new MaxAttemptStopStrategy(7).shouldRetry(new Exception(), 6, 10));
    }

    @Test
    public void TimeoutStopStrategyTest() {
        assert(new TimeoutStopStrategy().shouldRetry(new Exception(), 4, 1000));
        assert(!(new TimeoutStopStrategy().shouldRetry(new Exception(), 4, 11000)));
    }
}
