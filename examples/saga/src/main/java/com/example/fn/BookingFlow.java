package com.example.fn;

import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.Serializable;

public class BookingFlow implements Serializable {

    public static class BookingRequest implements Serializable{
        public String flightRequest;
        public String hotelRequest;
        public String carRentalRequest;
    }

    public static class BookingResult implements Serializable{
        public String flightConfirmation;
        public String hotelConfirmation;
        public String carRentalConfirmation;
    }

    private static final String API_BASE_URL = "http://172.17.0.4:3001";

    public void handleRequest(BookingRequest request) {

        CloudThreadRuntime rt = CloudThreads.currentRuntime();

        rt.supply(bookFlight(request.flightRequest))
            .thenCombine(
                rt.supply(bookHotel(request.hotelRequest)),
                (hotelConfirmation, flightConfirmation) -> {
                    BookingResult r = new BookingResult();
                    r.flightConfirmation = flightConfirmation;
                    r.hotelConfirmation = hotelConfirmation;
                    return r;
            }).thenCombine(
                    rt.supply(bookCarRental(request.carRentalRequest)),
                    (bookingResult, carRentalConfirmation) -> {
                        bookingResult.carRentalConfirmation = carRentalConfirmation;
                        return bookingResult;
            }).whenComplete(
                (result, exception)  -> {
                    if (exception == null) {
                        rt.supply(sendConfirmationMail(result));
                    } else {
                        rt.allOf(
                            Retry.exponentialWithJitter(cancelCarRental(request.carRentalRequest)),
                            Retry.exponentialWithJitter(cancelHotel(request.hotelRequest)),
                            Retry.exponentialWithJitter(cancelFlight(request.flightRequest))
                    );
                }
        });
    }

    private CloudThreads.SerCallable<String> sendConfirmationMail(BookingResult confirmations) {
        String message = String.format("Great success! Your trip is all booked. Here are your confirmation numbers:\nFlight: %s\nHotel: %s\nCar Rental: %s",
                confirmations.flightConfirmation, confirmations.hotelConfirmation, confirmations.carRentalConfirmation);
        return post("/email", message);
    }

    private CloudThreads.SerCallable<String>  bookCarRental(String request)  {
        return post("/car", request);
    }

    private CloudThreads.SerCallable<String>  bookHotel(String request) {
        return post("/hotel", request);
    }

    private CloudThreads.SerCallable<String>  bookFlight(String request) {
        return post("/flight", request);
    }

    private CloudThreads.SerCallable<String>  cancelCarRental(String request)  {
        return delete("/car");
    }

    private CloudThreads.SerCallable<String>  cancelHotel(String request) {
        return delete("/hotel");
    }

    private CloudThreads.SerCallable<String>  cancelFlight(String request) {
        return delete("/flight");
    }

    private static CloudThreads.SerCallable<String> post(String path, String body) {
        return () -> {
            try {
                return Request
                        .Post(API_BASE_URL + path)
                        .bodyString("{ \"request\": \"" + body + "\" }", ContentType.APPLICATION_JSON)
                        .execute().returnContent().asString();
            } catch (IOException e) {
                throw new RuntimeException("Failed to make HTTP request.");
            }
        };
    }

    private static CloudThreads.SerCallable<String> delete(String path) {
       return () -> {
            try {
                return Request
                        .Delete(API_BASE_URL + path)
                        .execute().returnContent().asString();
            } catch (IOException e) {
                throw new RuntimeException("Failed to make HTTP request.");
            }
        };
    }
}
