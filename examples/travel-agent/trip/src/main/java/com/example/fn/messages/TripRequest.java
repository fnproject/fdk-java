package com.example.fn.messages;

import java.io.Serializable;
import java.util.Date;

public class TripRequest implements Serializable {
    public FlightRequest flight;
    public HotelRequest hotel;
    public CarRentalRequest carRental;

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
}
