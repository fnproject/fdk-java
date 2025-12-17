package com.fnproject.fn.examples;


import com.fnproject.events.input.sch.MetricData;

public class MetricService {

    public void readMetric(MetricData metric) {
        System.out.println(metric);
        assert metric != null;
        assert metric.getDatapoints() != null && !metric.getDatapoints().isEmpty();
        assert metric.getCompartmentId() != null;
        assert metric.getDimensions() != null && !metric.getDimensions().isEmpty();
        assert metric.getMetadata() != null && !metric.getMetadata().isEmpty();
        assert metric.getName() != null && !metric.getName().isEmpty();
        assert metric.getNamespace() != null;
    }
}
