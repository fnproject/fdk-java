package com.example.fn.messages;

import java.io.Serializable;

public class TripRequest implements Serializable {
    public FlightRequest flightRequest;
    public HotelRequest hotelRequest;
    public CarRentalRequest carRentalRequest;
}
