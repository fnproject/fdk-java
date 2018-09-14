package com.fnproject.fn.integration.test2;

import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlainFunctionTest {

    @Rule
    public final FnTestingRule testing = FnTestingRule.createDefault();

    @Before
    public void setUp() {
        testing.setConfig("GREETING", "Howdy");
    }

    @Test
    public void shouldReturnGreeting() {
        testing.givenEvent().enqueue();
        testing.thenRun(PlainFunction.class, "handleRequest");

        FnResult result = testing.getOnlyResult();
        assertEquals("Howdy, world!", result.getBodyAsString());
    }

}
