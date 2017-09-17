package com.example.fn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;

import com.fnproject.fn.api.flow.HttpMethod;

import java.io.IOException;
import java.io.Serializable;

public class BookingFlow implements Serializable {

    private String flightConfirmation;
    private String hotelConfirmation;
    private String carConfirmation;

    public void handleRequest(ApiSchemas.BookingRequest request) {
        Flow f = Flows.currentFlow();

        f.invokeFunction("travel/flight/book", HttpMethod.POST, Headers.emptyHeaders(), getBytes(request.flight))
        .thenAccept((r0) -> {
            this.flightConfirmation = getConfirmationCode(r0.getBodyAsBytes());
            f.invokeFunction("travel/hotel/book", HttpMethod.POST, Headers.emptyHeaders(), getBytes(request.hotel))
                .thenAccept((r1) -> {
                    this.hotelConfirmation = getConfirmationCode(r1.getBodyAsBytes());
                    f.invokeFunction("travel/car/book", HttpMethod.POST, Headers.emptyHeaders(), getBytes(request.car))
                        .thenAccept((r2) -> {
                            this.carConfirmation = getConfirmationCode(r2.getBodyAsBytes());
                        });
                });
            }
        );

    }

    private String getConfirmationCode(byte[] bytes) {
        ObjectMapper mapper = new ObjectMapper();
        ApiSchemas.Confirmation r = null;
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

    private static final String API_BASE_URL = "http://172.17.0.4:3001";
}
