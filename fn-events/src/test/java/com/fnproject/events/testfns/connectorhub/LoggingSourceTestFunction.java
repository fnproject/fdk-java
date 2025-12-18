package com.fnproject.events.testfns.connectorhub;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.LoggingData;

public class LoggingSourceTestFunction extends ConnectorHubFunction<LoggingData> {

    @Override
    public void handler(ConnectorHubBatch<LoggingData> batch) {

    }
}
