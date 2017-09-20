package com.example.fn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.*;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class TripFunction implements Serializable {

    public void book(JsonObjects.TripRequest input) {
         invoke("./flight/book", input.flightRequest)
            .thenCompose((flight) -> invoke("./hotel/book", input.hotelRequest)
                .thenCompose((hotel) -> invoke("./car/book", input.carRentalRequest)
                    .exceptionallyCompose((r) -> invokeRetry("./car/cancel", input.carRentalRequest, 5)))
                .exceptionallyCompose((r) -> invokeRetry("./hotel/cancel", input.hotelRequest, 5)))
             .exceptionallyCompose((r) -> invokeRetry("./flight/cancel", input.flightRequest, 5))
         ;}


    private FlowFuture<JsonObjects.BookingResponse> invoke(String functionName, Object requestBody) {
        Flow f = Flows.currentFlow();
        return f.invokeFunction(functionName, HttpMethod.POST, Headers.emptyHeaders(), JsonObjects.toBytes(requestBody))
                .thenCompose((r) -> {
                    if (r.getStatusCode() == 200) {
                        return f.completedValue(JsonObjects.fromBytes(r.getBodyAsBytes(), new TypeReference<JsonObjects.BookingResponse>(){}));
                    } else {
                        return f.failedFuture(new RuntimeException());
                    }
                });
    }

    private FlowFuture<JsonObjects.BookingResponse> invokeRetry(String functionName, Object requestBody, int triesLeft) {
        Flow f = Flows.currentFlow();
        return invoke(functionName, requestBody)
                .exceptionallyCompose((e) -> {
                    if (triesLeft > 0) {
                        return f.delay(5, TimeUnit.SECONDS).thenCompose((a) -> invokeRetry(functionName, requestBody, triesLeft - 1));
                    } else {
                        return f.failedFuture(new RuntimeException("Gave up"));
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