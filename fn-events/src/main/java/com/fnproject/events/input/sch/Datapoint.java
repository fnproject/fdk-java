package com.fnproject.events.input.sch;

import java.util.Date;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Datapoint {
    private final Date timestamp;
    private final Double value;
    private final Integer count;

    @JsonCreator
    public Datapoint(
        @JsonProperty("timestamp") Date timestamp,
        @JsonProperty("value") Double value,
        @JsonProperty("count") Integer count) {
        this.timestamp = timestamp;
        this.value = value;
        this.count = count;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Double getValue() {
        return value;
    }

    public Integer getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Datapoint datapoint = (Datapoint) o;
        return Objects.equals(timestamp, datapoint.timestamp) && Objects.equals(value, datapoint.value) && Objects.equals(count, datapoint.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value, count);
    }

    @Override
    public String toString() {
        return "Datapoint{" +
            "timestamp=" + timestamp +
            ", value=" + value +
            ", count=" + count +
            '}';
    }

}