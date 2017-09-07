package com.example.fn;

import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;

import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;

public class BookingFlow {

    public class BookingRequest {
        public String FlightNumber;
        public String HotelName;
        public String CarModel;
    }

    public void handleRequest() {

        CloudThreadRuntime rt = CloudThreads.currentRuntime();

        rt.allOf( rt.supply(() -> Request
                        .Get("http://172.17.0.4:3001/flight")
                        .execute().returnContent().asString())
                ,
                rt.supply( () -> Request
                        .Post("http://172.17.0.4:3001/hotel")
                        .execute().returnContent().asString())
                ,
                rt.supply( () -> Request
                        .Post("http://172.17.0.4:3001/car")
                        .execute().returnContent().asString())
            );

    }
}
