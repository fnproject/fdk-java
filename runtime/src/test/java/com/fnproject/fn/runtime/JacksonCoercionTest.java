package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.runtime.coercion.jackson.JacksonCoercion;
import com.fnproject.fn.runtime.testfns.Animal;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JacksonCoercionTest {

    public String testMethod(List<Animal> ss) {
        // This method isn't actually called, it only exists to have its parameter types examined by the JacksonCoercion
        return ss.get(0).getName();
    }

    @Test
    public void listOfCustomObjects() throws NoSuchMethodException {
        JacksonCoercion jc = new JacksonCoercion();

        MethodWrapper method = new DefaultMethodWrapper(JacksonCoercionTest.class, "testMethod");
        FunctionRuntimeContext frc = new FunctionRuntimeContext(method, new HashMap<>());
        FunctionInvocationContext invocationContext = new FunctionInvocationContext(frc,new ReadOnceInputEvent(new ByteArrayInputStream(new byte[0]),Headers.emptyHeaders(),"callID",Instant.now()));

        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");

        ByteArrayInputStream body = new ByteArrayInputStream("[{\"name\":\"Spot\",\"age\":6},{\"name\":\"Jason\",\"age\":16}]".getBytes());
        InputEvent inputEvent = new ReadOnceInputEvent( body, Headers.fromMap(headers),"call",Instant.now());

        Optional<Object> object = jc.tryCoerceParam(invocationContext, 0, inputEvent, method);

        List<Animal> animals = (List<Animal>) (object.get());
        Animal first = animals.get(0);

        Assert.assertEquals("Spot", first.getName());
    }

    @Test
    public void failureToParseIsUserFriendlyError() throws NoSuchMethodException {
        JacksonCoercion jc = new JacksonCoercion();

        MethodWrapper method = new DefaultMethodWrapper(JacksonCoercionTest.class, "testMethod");
        FunctionRuntimeContext frc = new FunctionRuntimeContext(method, new HashMap<>());
        FunctionInvocationContext invocationContext = new FunctionInvocationContext(frc,new ReadOnceInputEvent(new ByteArrayInputStream(new byte[0]),Headers.emptyHeaders(),"callID",Instant.now()));

        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");

        ByteArrayInputStream body = new ByteArrayInputStream("INVALID JSON".getBytes());
        InputEvent inputEvent = new ReadOnceInputEvent( body, Headers.fromMap(headers), "call",Instant.now());

        boolean causedExpectedError;
        try {
            Optional<Object> object = jc.tryCoerceParam(invocationContext, 0, inputEvent, method);
            causedExpectedError = false;
        } catch (RuntimeException e) {
            causedExpectedError = true;
            Assert.assertEquals("Failed to coerce event to user function parameter type java.util.List<com.fnproject.fn.runtime.testfns.Animal>", e.getMessage());
            Assert.assertTrue(e.getCause().getMessage().startsWith("Unrecognized token 'INVALID':"));
        }
        Assert.assertTrue(causedExpectedError);
    }
}
