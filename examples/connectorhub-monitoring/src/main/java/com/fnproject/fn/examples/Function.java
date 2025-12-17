package com.fnproject.fn.examples;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.MetricData;

public class Function extends ConnectorHubFunction<MetricData> {

    public MetricService metricService;

    public Function() {
        this.metricService = new MetricService();
    }

    @Override
    public void handler(ConnectorHubBatch<MetricData> batch) {
        for (MetricData metric : batch.getBatch()) {
            metricService.readMetric(metric);
        }
    }
}