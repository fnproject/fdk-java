package com.example.fn;

import com.example.fn.messages.*;
import com.fnproject.fn.api.flow.FlowFuture;

public class Functions {
    public static FlowFuture<BookingResponse> bookFlight(FlightRequest req) {
        return JsonHelper.wrapJsonFunction("./flight/book", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> bookHotel(HotelRequest req) {
        return JsonHelper.wrapJsonFunction("./hotel/book", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> bookCar(CarRentalRequest req) {
        return JsonHelper.wrapJsonFunction("./car/book", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> cancelFlight(FlightRequest req) {
        return JsonHelper.wrapJsonFunction("./flight/cancel", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> cancelHotel(HotelRequest req) {
        return JsonHelper.wrapJsonFunction("./hotel/cancel", req, BookingResponse.class);
    }

    public static FlowFuture<BookingResponse> cancelCar(CarRentalRequest req) {
        return JsonHelper.wrapJsonFunction("./car/cancel", req, BookingResponse.class);
    }

    public static FlowFuture<Void> sendEmail(EmailRequest req) {
        return JsonHelper.wrapJsonFunction("./email", req);
    }
}
