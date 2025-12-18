package com.fnproject.events;

import static org.junit.Assert.assertEquals;
import com.fnproject.events.testfns.notification.NotificationObjectTestFunction;
import com.fnproject.events.testfns.notification.NotificationStringTestFunction;
import com.fnproject.fn.testing.FnTestingRule;
import org.junit.Rule;
import org.junit.Test;

public class NotificationFunctionTest {

    @Rule
    public final FnTestingRule fnRule = FnTestingRule.createDefault();

    @Test
    public void testNotificationTestFunction() {
        fnRule
            .givenEvent()
            .withBody("{\"name\":\"foo\",\"age\":3}")
            .enqueue();

        fnRule.thenRun(NotificationObjectTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testNotificationStringTestFunction() {
        fnRule
            .givenEvent()
            .withBody("test string")
            .enqueue();

        fnRule.thenRun(NotificationStringTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testBlankNotificationStringTestFunction() {
        fnRule
            .givenEvent()
            .withBody("")
            .enqueue();

        fnRule.thenRun(NotificationStringTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }

    @Test
    public void testBlankNotificationWithoutBodyTestFunction() {
        fnRule
            .givenEvent()
            .enqueue();

        fnRule.thenRun(NotificationStringTestFunction.class, "handler");

        int exitCode = fnRule.getLastExitCode();
        assertEquals(0, exitCode);
    }
}