package com.fnproject.fn.api.flow;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FlowsTest {
    public FlowsTest() {
    }

    /** People shall not be allowed to create subclasses of {@code Flow}:
    * <pre>
    * static class MyFlows extends Flows {
    * }
    * </pre>
    */
    @Test
    public void dontSubclassFlows() {
        assertTrue("Flows is final", Modifier.isFinal(Flows.class.getModifiers()));
        assertEquals("No visible constructors", 0, Flows.class.getConstructors().length);
    }
}
