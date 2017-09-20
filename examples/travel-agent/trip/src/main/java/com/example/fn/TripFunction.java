package com.example.fn;

import com.example.fn.messages.BookingResponse;
import com.example.fn.messages.EmailRequest;
import com.example.fn.messages.TripRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class TripFunction implements Serializable {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
    }

    private static final Logger log = LoggerFactory.getLogger(TripFunction.class);

    public void book(TripRequest input) {
         Functions.bookFlight(input.flightRequest)
            .thenCompose((flight) -> Functions.bookHotel(input.hotelRequest)
                .thenCompose((hotel) -> Functions.bookCar(input.carRentalRequest)
                    .whenComplete((car, e) -> Functions.sendEmail(composeEmail(flight, hotel, car))))
                    .exceptionallyCompose((r) -> Retry.retryThenFail(()->Functions.cancelCar(input.carRentalRequest)))
                .exceptionallyCompose((r) -> Retry.retryThenFail(()->Functions.cancelHotel(input.hotelRequest)))
             .exceptionallyCompose((r) -> Retry.retryThenFail(()->Functions.cancelFlight(input.flightRequest))))
         .exceptionally((e) -> {Functions.sendEmail(new EmailRequest());return null;});
    }

    private EmailRequest composeEmail(BookingResponse flightResponse,
                                      BookingResponse hotelResponse,
                                      BookingResponse carResponse) {
        EmailRequest result = new EmailRequest();
        result.message = flightResponse.confirmation + hotelResponse.confirmation + carResponse.confirmation;  ;
        return result;
    }
}