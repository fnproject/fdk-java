package com.example.fn;

import java.util.Date;

public class ApiSchemas {
    public static class BookingRequest {
        public ApiSchemas.FlightRequest flight;
        public ApiSchemas.HotelRequest hotel;
        public ApiSchemas.CarRequest car;
    }

    public static class FlightRequest {
        public Date departureTime;
        public String flightCode;
    }

    public static class HotelRequest {
        public String city;
        public String hotel;
    }

    public static class CarRequest {
        public String model;
    }

    public static class Confirmation {
        public String confirmation;
    }
}
