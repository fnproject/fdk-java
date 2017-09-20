package com.example.fn;

import com.example.fn.messages.BookingResponse;
import com.example.fn.messages.EmailRequest;
import com.example.fn.messages.TripRequest;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.function.Function;

public class TripFunction implements Serializable {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
    }

    private static final Logger log = LoggerFactory.getLogger(TripFunction.class);

    public void book(TripRequest input) {
        Flow f = Flows.currentFlow();
        Functions.bookFlight(input.flight)
            .thenCompose((flightResponse) -> Functions.bookHotel(input.hotel)
                .thenCompose((hotelResponse) -> Functions.bookCar(input.carRental)
                    .whenComplete((carResponse,r)->Functions.sendEmail(composeEmail(flightResponse, hotelResponse, carResponse)))
                    .exceptionallyCompose((e) -> {Retry.exponentialWithJitter(() -> Functions.cancelCar(input.carRental));return f.failedFuture(e);}))
                .exceptionallyCompose((e) -> {Retry.exponentialWithJitter(() -> Functions.cancelHotel(input.hotel));return f.failedFuture(e);}))
            .exceptionallyCompose((e) -> {Retry.exponentialWithJitter(() -> Functions.cancelFlight(input.flight));return f.failedFuture(e);})
        .exceptionally((e) -> {Functions.sendEmail(new EmailRequest());return null;});
    }

    private EmailRequest composeEmail(BookingResponse flightResponse,
                                      BookingResponse hotelResponse,
                                      BookingResponse carResponse) {
        EmailRequest result = new EmailRequest();
        result.message = "Flight confirmation: " + flightResponse.confirmation + "\n" +
                         "Hotel confirmation: " +  hotelResponse.confirmation + "\n" +
                         "Car rental confirmation: " + carResponse.confirmation;
        return result;
    }
}