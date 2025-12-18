package com.fnproject.events.testfns.connectorhub;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.MetricData;

public class MonitorSourceTestFunction extends ConnectorHubFunction<MetricData> {

    @Override
    public void handler(ConnectorHubBatch<MetricData> batch) {

    }
}
