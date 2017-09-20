package com.example.fn;

import com.example.fn.messages.*;
import com.fnproject.fn.api.flow.FlowFuture;

public class Functions {
    public static FlowFuture<BookingResponse> bookFlight(TripRequest.FlightRequest req) {
        return JsonHelper.wrapJsonFunction("./flight/book", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> bookHotel(TripRequest.HotelRequest req) {
        return JsonHelper.wrapJsonFunction("./hotel/book", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> bookCar(TripRequest.CarRentalRequest req) {
        return JsonHelper.wrapJsonFunction("./car/book", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> cancelFlight(TripRequest.FlightRequest req) {
        return JsonHelper.wrapJsonFunction("./flight/cancel", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> cancelHotel(TripRequest.HotelRequest req) {
        return JsonHelper.wrapJsonFunction("./hotel/cancel", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> cancelCar(TripRequest.CarRentalRequest req) {
        return JsonHelper.wrapJsonFunction("./car/cancel", req, BookingResponse.class);
    }

    public static FlowFuture<Void> sendEmail(EmailRequest req) {
        return JsonHelper.wrapJsonFunction("./email", req);
    }
}
