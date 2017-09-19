package com.example.fn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.testing.*;
import org.junit.*;

import java.util.Date;

import static org.junit.Assert.*;

public class FlightFunctionTest {

    private static final FlightFunction.FlightBookingRequest test_data = new FlightFunction.FlightBookingRequest();
    private static final ObjectMapper defaultMapper = new ObjectMapper();
    {
        test_data.departureTime = new Date(System.currentTimeMillis());
        test_data.flightCode = "BA12345";
    }

    private void setupGoodData() throws JsonProcessingException {
        testing.givenEvent().withBody(defaultMapper.writeValueAsBytes(test_data)).enqueue();
    }

    private void setupGoodConfig() {
        testing.setConfig("FLIGHT_API_URL", "http://localhost:3000/flight");
        testing.setConfig("FLIGHT_API_SECRET", "shhh");
    }

    @Rule
    public final FnTestingRule testing = FnTestingRule.createDefault();

    @Test
    public void shouldRejectNullInput() {
        setupGoodConfig();
        testing.givenEvent().enqueue();
        testing.thenRun(FlightFunction.class, "book");
        String stderr = testing.getStdErrAsString();

        String expected = "An exception was thrown during Input Coercion:";
        assertEquals(expected, stderr.substring(0, expected.length()));
    }

    @Test
    @Ignore
    public void shouldAcceptValidInput() throws JsonProcessingException {
        setupGoodData();
        setupGoodConfig();
        testing.thenRun(FlightFunction.class, "book");
        String stderr = testing.getStdErrAsString();
        assertEquals("", stderr);
    }

    @Test
    public void shouldFailWithNoConfig() throws JsonProcessingException {
        setupGoodData();
        testing.thenRun(FlightFunction.class, "book");
        String stderr = testing.getStdErrAsString();

        String expected = "Error invoking configuration method: configure\n" +
                "Caused by: java.lang.RuntimeException: No URL endpoint was provided.";
        assertEquals(expected, stderr.substring(0, expected.length()));
    }
}