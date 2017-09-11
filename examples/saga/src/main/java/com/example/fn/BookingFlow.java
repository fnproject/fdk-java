package com.example.fn;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

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

        Flow rt = Flows.currentFlow();

        rt.supply(post("/flight", request.flightRequest))
            .thenCombine(
                rt.supply(post("/hotel", request.hotelRequest)),
                (flightConfirmation, hotelConfirmation) -> {
                    BookingResult r = new BookingResult();
                    r.flightConfirmation = flightConfirmation;
                    r.hotelConfirmation = hotelConfirmation;
                    return r;
            }).thenCombine(
                    rt.supply(post("/car", request.carRentalRequest)),
                    (bookingResult, carRentalConfirmation) -> {
                        bookingResult.carRentalConfirmation = carRentalConfirmation;
                        return bookingResult;
            }).whenComplete(
                (result, exception)  -> {
                    if (exception == null) {
                        String message = String.format("Great success! Your trip is all booked. Here are your confirmation numbers - Flight: %s; Hotel: %s; Car Rental: %s.",
                                result.flightConfirmation, result.hotelConfirmation, result.carRentalConfirmation);
                        rt.supply(post("/email", message));
                    } else {
                        rt.allOf(
                                Retry.exponentialWithJitter(delete("/car")),
                                Retry.exponentialWithJitter(delete("/hotel")),
                                Retry.exponentialWithJitter(delete("/flight"))
                    ).whenComplete((r,e) -> post("/email", "We failed to books your trip. LOL."));
                }
        });
    }

    private static Flows.SerCallable<String> post(String path, String body) {
        return () -> Request.Post(API_BASE_URL + path)
                        .bodyString("{ \"request\": \"" + body + "\" }", ContentType.APPLICATION_JSON)
                        .execute().returnContent().asString().replace("\"", "");
    }

    private static Flows.SerCallable<String> delete(String path) {
        return () -> Request.Delete(API_BASE_URL + path)
                        .execute().returnContent().asString();
    }
}
