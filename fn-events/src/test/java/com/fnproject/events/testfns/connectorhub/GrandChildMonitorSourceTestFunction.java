package com.fnproject.events.testfns.connectorhub;

import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.MetricData;

public class GrandChildMonitorSourceTestFunction extends MonitorSourceTestFunction {

    @Override
    public void handler(ConnectorHubBatch<MetricData> batch) {
        super.handler(batch);
    }
}
