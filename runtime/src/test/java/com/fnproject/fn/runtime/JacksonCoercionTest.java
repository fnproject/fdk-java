package com.fnproject.fn.runtime;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.runtime.coercion.jackson.JacksonCoercion;
import com.fnproject.fn.runtime.testfns.Animal;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.*;

public class JacksonCoercionTest {

    public String testMethod(List<Animal> ss) {
        // This method isn't actually called, it only exists to have its parameter types examined by the JacksonCoercion
        return ss.get(0).getName();
    }

    @Test
    public void listOfCustomObjects() throws NoSuchMethodException {
        JacksonCoercion jc = new JacksonCoercion();

        FunctionRuntimeContext frc = new FunctionRuntimeContext(JacksonCoercionTest.class, JacksonCoercionTest.class.getMethod("testMethod", List.class), new HashMap<>());
        FunctionInvocationContext invocationContext = new FunctionInvocationContext(frc);

        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");

        ByteArrayInputStream body = new ByteArrayInputStream("[{\"name\":\"Spot\",\"age\":6},{\"name\":\"Jason\",\"age\":16}]".getBytes());
        InputEvent inputEvent = new ReadOnceInputEvent("", "", "", "testMethod","call-id", body, Headers.fromMap(headers), new QueryParametersImpl());

        Optional<Object> object = jc.tryCoerceParam(invocationContext, 0, inputEvent);

        List<Animal> animals = (List<Animal>) (object.get());
        Animal first = animals.get(0);

        Assert.assertEquals("Spot", first.getName());
    }

    @Test
    public void failureToParseIsUserFriendlyError() throws NoSuchMethodException {
        JacksonCoercion jc = new JacksonCoercion();

        FunctionRuntimeContext frc = new FunctionRuntimeContext(JacksonCoercionTest.class, JacksonCoercionTest.class.getMethod("testMethod", List.class), new HashMap<>());
        FunctionInvocationContext invocationContext = new FunctionInvocationContext(frc);

        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");

        ByteArrayInputStream body = new ByteArrayInputStream("INVALID JSON".getBytes());
        InputEvent inputEvent = new ReadOnceInputEvent("", "", "", "testMethod","call-id",body, Headers.fromMap(headers), new QueryParametersImpl());

        boolean causedExpectedError;
        try {
            Optional<Object> object = jc.tryCoerceParam(invocationContext, 0, inputEvent);
            causedExpectedError = false;
        } catch (RuntimeException e) {
            causedExpectedError = true;
            Assert.assertEquals("Failed to coerce event to user function parameter type java.util.List<com.fnproject.fn.runtime.testfns.Animal>", e.getMessage());
            Assert.assertTrue(e.getCause().getMessage().startsWith("Unrecognized token 'INVALID':"));
        }
        Assert.assertTrue(causedExpectedError);
    }
}
