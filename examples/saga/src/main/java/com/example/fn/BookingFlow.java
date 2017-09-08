package com.example.fn;

import com.fnproject.fn.api.cloudthreads.CloudFuture;
import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;

import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.io.Serializable;

public class BookingFlow implements Serializable {

    public static class BookingRequest implements Serializable{
        public String flightRequest;
        public String hotelRequest;
        public String carRentalRequest;
    }

    private final String API_BASE_URL = "http://172.17.0.4:3001";

    public void handleRequest(BookingRequest request) {

        CloudThreadRuntime rt = CloudThreads.currentRuntime();

        rt.allOf(
                bookFlight(request.flightRequest),
                bookHotel(request.hotelRequest),
                bookCarRental(request.carRentalRequest)
        ).exceptionally( (exception) ->
            rt.allOf(
                cancelFlight(request.flightRequest),
                cancelHotel(request.hotelRequest),
                cancelCarRental(request.carRentalRequest)
            ).get()
        );
    }

    private CloudFuture<String> bookCarRental(String request)  {
        return CloudThreads.currentRuntime().supply( () ->
                Request
                .Post(API_BASE_URL + "/car")
                .execute().returnContent().asString());
    }

    private CloudFuture<String> bookHotel(String request) {
        return CloudThreads.currentRuntime().supply( () ->
                Request
                .Post(API_BASE_URL + "/hotel")
                .execute().returnContent().asString());
    }

    private CloudFuture<String> bookFlight(String request) {
        return CloudThreads.currentRuntime().supply( () ->
                Request
                .Post(API_BASE_URL + "/flight")
                .execute().returnContent().asString());
    }

    private CloudFuture<String> cancelCarRental(String request)  {
        return CloudThreads.currentRuntime().supply( () ->
                Request
                .Delete(API_BASE_URL + "/car")
                .execute().returnContent().asString());
    }

    private CloudFuture<String> cancelHotel(String request) {
        return CloudThreads.currentRuntime().supply( () ->
                Request
                .Delete(API_BASE_URL + "/hotel")
                .execute().returnContent().asString());
    }

    private CloudFuture<String> cancelFlight(String request) {
        return CloudThreads.currentRuntime().supply( () ->
                Request
                .Delete(API_BASE_URL + "/flight")
                .execute().returnContent().asString());
    }
}
