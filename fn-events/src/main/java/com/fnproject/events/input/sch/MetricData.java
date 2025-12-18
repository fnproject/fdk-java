package com.fnproject.events.input.sch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricData {
    private final String namespace;
    private final String resourceGroup;
    private final String compartmentId;
    private final String name;
    private final Map<String, String> dimensions;
    private final Map<String, String> metadata;
    private final List<Datapoint> datapoints;

    @JsonCreator
    public MetricData(
        @JsonProperty("namespace") String namespace,
        @JsonProperty("resourceGroup") String resourceGroup,
        @JsonProperty("compartmentId") String compartmentId,
        @JsonProperty("name") String name,
        @JsonProperty("dimensions") Map<String, String> dimensions,
        @JsonProperty("metadata") Map<String, String> metadata,
        @JsonProperty("datapoints") List<Datapoint> datapoints) {

        this.namespace = namespace;
        this.resourceGroup = resourceGroup;
        this.compartmentId = compartmentId;
        this.name = name;
        this.dimensions = dimensions;
        this.metadata = metadata;
        this.datapoints = datapoints;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getCompartmentId() {
        return compartmentId;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getDimensions() {
        return dimensions;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public List<Datapoint> getDatapoints() {
        return datapoints;
    }


    @Override
    public String toString() {
        return "MetricData{" +
            "namespace='" + namespace + '\'' +
            ", resourceGroup='" + resourceGroup + '\'' +
            ", compartmentId='" + compartmentId + '\'' +
            ", name='" + name + '\'' +
            ", dimensions=" + dimensions +
            ", metadata=" + metadata +
            ", datapoints=" + datapoints +
            '}';
    }

}