package com.fnproject.events.input.sch;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggingData {
    private final String id;
    private final String source;
    private final String specversion;
    private final String subject;
    private final String type;
    private final Map<String, String> data;
    private final Map<String, String> oracle;
    private final Date time;

    @JsonCreator
    public LoggingData(
        @JsonProperty("id") String id,
        @JsonProperty("source") String source,
        @JsonProperty("specversion") String specversion,
        @JsonProperty("subject") String subject,
        @JsonProperty("type") String type,
        @JsonProperty("data") Map<String, String> data,
        @JsonProperty("oracle") Map<String, String> oracle,
        @JsonProperty("time") Date time) {

        this.id = id;
        this.source = source;
        this.specversion = specversion;
        this.subject = subject;
        this.type = type;
        this.data = data;
        this.oracle = oracle;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getSpecversion() {
        return specversion;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, String> getData() {
        return data;
    }

    public Map<String, String> getOracle() {
        return oracle;
    }

    public Date getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LoggingData that = (LoggingData) o;
        return Objects.equals(id, that.id) && Objects.equals(source, that.source) && Objects.equals(specversion, that.specversion) &&
            Objects.equals(subject, that.subject) && Objects.equals(type, that.type) && Objects.equals(data, that.data) &&
            Objects.equals(oracle, that.oracle) && Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, source, specversion, subject, type, data, oracle, time);
    }

    @Override
    public String toString() {
        return "LoggingData{" +
            "id='" + id + '\'' +
            ", source='" + source + '\'' +
            ", specversion='" + specversion + '\'' +
            ", subject='" + subject + '\'' +
            ", type='" + type + '\'' +
            ", data=" + data +
            ", oracle=" + oracle +
            ", time=" + time +
            '}';
    }
}