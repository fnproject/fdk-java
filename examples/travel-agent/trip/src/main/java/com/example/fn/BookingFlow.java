package com.example.fn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

import com.fnproject.fn.api.flow.HttpMethod;
import com.fnproject.fn.api.flow.HttpResponse;

import java.io.IOException;

public class BookingFlow {

    public void handleRequest(ApiSchemas.BookingRequest request) {
        invokeFunction("./flight/book", request.flight)
            .thenCompose((flightConfirmation) -> invokeFunction("./hotel/book", request.hotel)
                .thenCompose((hotelConfirmation) -> invokeFunction("./car/book", request.car)
                    .exceptionallyCompose((e) -> retryInvokeFunction(e, "./car/cancel", request.car))
                    .thenCompose((carConfirmation) -> invokeFunction("./email", getSuccessEmailBody(flightConfirmation, hotelConfirmation, carConfirmation))))
                .exceptionallyCompose((e) -> retryInvokeFunction(e, "./hotel/cancel", request.hotel))
            .exceptionallyCompose((e) -> retryInvokeFunction(e, "./flight/cancel", request.flight))
            .whenComplete((r1, e1) -> invokeFunction("./email", FAIL_EMAIL_BODY))
        );
    }

    private FlowFuture<String> retryInvokeFunction(Throwable e, String functionName, Object requestBody) {

    return Retry.exponentialWithJitter(() -> invokeFunction(functionName, requestBody))
            .thenCompose((r) -> Flows.currentFlow().failedFuture(e));
    }

    private FlowFuture<String> invokeFunction(String functionName, Object requestBody) {
        return Flows.currentFlow().invokeFunction(functionName, HttpMethod.POST, Headers.emptyHeaders(), getBytes(requestBody))
                .thenApply((r) -> getConfirmationCode(r.getBodyAsBytes()));
    }

    private String getConfirmationCode(byte[] bytes) {
        ObjectMapper mapper = new ObjectMapper();
        ApiSchemas.Confirmation r;
        try {
            r = mapper.readValue(bytes, ApiSchemas.Confirmation.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return r.confirmation;
    }

    private byte[] getBytes(Object request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsBytes(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String SUCCESS_EMAIL_BODY = "Great success! Your trip is all booked. Here are your confirmation numbers - Flight: %s; Hotel: %s; Car Rental: %s.";
    private static final String FAIL_EMAIL_BODY = "We failed to books your trip. LOL.";

    private String getSuccessEmailBody(String flightConfirmation, String hotelConfirmation, String carRentalConfirmation) {
        return String.format(SUCCESS_EMAIL_BODY,
                flightConfirmation, hotelConfirmation, carRentalConfirmation);
    }
}
