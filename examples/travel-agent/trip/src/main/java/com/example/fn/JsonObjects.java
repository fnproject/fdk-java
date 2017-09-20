package com.example.fn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

public class JsonObjects implements Serializable {
    public static class TripRequest implements Serializable {
        public FlightRequest flightRequest;
        public HotelRequest hotelRequest;
        public CarRentalRequest carRentalRequest;
    }

    public static class FlightRequest implements Serializable {
        public Date departureTime;
        public String flightCode;
    }

    public static class HotelRequest implements Serializable {
        public String city;
        public String hotel;
    }

    public static class CarRentalRequest implements Serializable {
        public String model;
    }

    public static class BookingResponse implements Serializable {
        public String confirmation;
    }

    public static byte[] toBytes(Object input) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsBytes(input);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static BookingResponse fromBytes(byte[] bytes) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(bytes, BookingResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
