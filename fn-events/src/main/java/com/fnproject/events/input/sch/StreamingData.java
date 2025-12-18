package com.fnproject.events.input.sch;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fnproject.events.coercion.jackson.Base64ToTypeDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamingData<T> {
    private final String stream;
    private final String partition;
    private final String key;
    private final T value;
    private final String offset;
    private final Date timestamp;

    @JsonCreator
    public StreamingData(
        @JsonProperty("stream") String stream,
        @JsonProperty("partition") String partition,
        @JsonProperty("key") String key,
        @JsonDeserialize(using = Base64ToTypeDeserializer.class) @JsonProperty("value") T value,
        @JsonProperty("offset") String offset,
        @JsonProperty("time") Date timestamp) {
        this.stream = stream;
        this.partition = partition;
        this.key = key;
        this.value = value;
        this.offset = offset;
        this.timestamp = timestamp;
    }

    public String getStream() {
        return stream;
    }

    public String getPartition() {
        return partition;
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getOffset() {
        return offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StreamingData<?> that = (StreamingData<?>) o;
        return Objects.equals(stream, that.stream) && Objects.equals(partition, that.partition) && Objects.equals(key, that.key) &&
            Objects.equals(value, that.value) && Objects.equals(offset, that.offset) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stream, partition, key, value, offset, timestamp);
    }

    @Override
    public String toString() {
        return "StreamingData{" +
            "stream='" + stream + '\'' +
            ", partition='" + partition + '\'' +
            ", key='" + key + '\'' +
            ", value=" + value +
            ", offset='" + offset + '\'' +
            ", timestamp=" + timestamp +
            '}';
    }
}