package com.example.fn;
import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;

import com.goplacesairlines.api.GoPlacesAirlines;

import java.io.Serializable;
import java.util.Date;

public class FlightBooker implements Serializable {

    private GoPlacesAirlines apiClient;

    public static class FlightBookingRequest implements Serializable {
        public String flightCode;
        public Date departureTime;
    }

    public static class FlightBookingResponse implements Serializable {
        public FlightBookingResponse(String confirmation) {
            this.confirmation = confirmation;
        }

        public String confirmation;
    }

    @FnConfiguration
    public void configure(RuntimeContext ctx) {
        String airlineApiUrl = ctx.getConfigurationByKey("AIRLINE_API_URL")
                .orElse("http://localhost:3000");

        String airlineApiSecret = ctx.getConfigurationByKey("AIRLINE_API_SECRET")
                .orElseThrow(() -> new RuntimeException("No credentials provided for airline API."));

        this.apiClient = new GoPlacesAirlines(airlineApiUrl, airlineApiSecret);
    }

    public FlightBookingResponse book(FlightBookingRequest flightDetails) {
        GoPlacesAirlines.BookingResponse apiResponse  = apiClient.bookFlight(flightDetails.flightCode, flightDetails.departureTime);
        return new FlightBookingResponse(apiResponse.confirmation);
    }
}
