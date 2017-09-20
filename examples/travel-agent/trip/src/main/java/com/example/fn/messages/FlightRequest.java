package com.example.fn.messages;

import java.io.Serializable;
import java.util.Date;

public class FlightRequest implements Serializable {
    public Date departureTime;
    public String flightCode;
}
