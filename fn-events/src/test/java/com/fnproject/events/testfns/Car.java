package com.fnproject.events.testfns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Car {
    private final String brand;
    private final int wheels;

    @JsonCreator
    public Car(@JsonProperty("brand") String brand,
                  @JsonProperty("wheels") int wheels) {
        this.brand = brand;
        this.wheels = wheels;
    }

    public int getWheels() {
        return wheels;
    }

    public String getBrand() {
        return brand;
    }
}
