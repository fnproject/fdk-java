package com.example.fn;

import java.io.Serializable;
import java.util.Date;

public class ApiSchemas {
    public static class BookingRequest implements Serializable {
        public ApiSchemas.FlightRequest flight;
        public ApiSchemas.HotelRequest hotel;
        public ApiSchemas.CarRequest car;
    }

    public static class FlightRequest implements Serializable {
        public Date departureTime;
        public String flightCode;
    }

    public static class HotelRequest implements Serializable {
        public String city;
        public String hotel;
    }

    public static class CarRequest implements Serializable {
        public String model;
    }

    public static class Confirmation implements Serializable {
        public String confirmation;
    }
}
