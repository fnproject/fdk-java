package com.goplacesairlines.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

public class GoPlacesAirlines implements Serializable {
    public GoPlacesAirlines(String airlineApiUrl, String secret) {
        this.airlineApiUrl = airlineApiUrl;
        this.secret = secret;
    }

    private String secret;
    private String airlineApiUrl;

    public BookingResponse bookFlight(String flightCode, Date departureTime) {
        ObjectMapper jsonify = new ObjectMapper();
        BookFlightInfo request = new BookFlightInfo();
        request.flightCode = flightCode;
        request.departureTime = departureTime;
        request.secret = this.secret;
        try {
            String response = Request.Post(airlineApiUrl + "/flight")
                    .bodyByteArray(
                            jsonify.writeValueAsBytes(request),
                            ContentType.APPLICATION_JSON)
                    .execute().returnContent().asString();
            return jsonify.readValue(response, BookingResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class BookFlightInfo implements Serializable {
        public String flightCode;
        public Date departureTime;
        public String secret;
    }

    public static class BookingResponse implements Serializable {
        public String reference;
    }
}
