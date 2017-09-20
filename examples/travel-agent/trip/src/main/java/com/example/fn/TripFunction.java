package com.example.fn;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;

import java.io.Serializable;

public class TripFunction implements Serializable {

    public void book(JsonObjects.TripRequest input) {
         invoke("./flight/book", input.flightRequest)
            .thenCompose((flight) -> invoke("./hotel/book", input.hotelRequest)
                .thenCompose((hotel) -> invoke("./car/book", input.carRentalRequest)
                    .exceptionallyCompose((r) -> retryThenFail("./car/cancel", input.carRentalRequest))
                    .thenAccept((car) -> invokeEmailFunction(composeEmail(flight, hotel, car))))
                .exceptionallyCompose((r) -> retryThenFail("./hotel/cancel", input.hotelRequest)))
             .exceptionallyCompose((r) -> retryThenFail("./flight/cancel", input.flightRequest))
         .exceptionally((e) -> {invokeEmailFunction("{\"message\": \"fail\"}");return null;});
    }

    private <T> FlowFuture<T> retryThenFail(String functionName, Object requestBody) {
        return Retry.exponentialWithJitter(() -> invoke(functionName, requestBody))
                .thenCompose((j) -> Flows.currentFlow().failedFuture(new RuntimeException()));
    }

    private FlowFuture<JsonObjects.BookingResponse> invoke(String functionName, Object requestBody) {
        Flow f = Flows.currentFlow();
        return f.invokeFunction(functionName, HttpMethod.POST, Headers.emptyHeaders(), JsonObjects.toBytes(requestBody))
                .thenApply((r) -> {
                    if (r.getStatusCode() == 200) {
                        return JsonObjects.fromBytes(r.getBodyAsBytes());
                    } else {
                        throw new RuntimeException();
                    }
                });
    }

    private FlowFuture<Void> invokeEmailFunction(String requestBody){
        return Flows.currentFlow().invokeFunction("./email", HttpMethod.POST, Headers.emptyHeaders(), requestBody.getBytes())
                .thenAccept((a)->{});
    }

    private String composeEmail(JsonObjects.BookingResponse flightResponse,
                         JsonObjects.BookingResponse hotelResponse,
                         JsonObjects.BookingResponse carResponse) {
        return "{\"message\": \"" + flightResponse.confirmation + hotelResponse.confirmation + carResponse.confirmation + "\"}";
    }
}