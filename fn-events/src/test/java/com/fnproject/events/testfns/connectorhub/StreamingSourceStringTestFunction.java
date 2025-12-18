package com.fnproject.events.testfns.connectorhub;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.StreamingData;
import com.fnproject.events.testfns.Animal;

public class StreamingSourceStringTestFunction extends ConnectorHubFunction<StreamingData<String >> {

    @Override
    public void handler(ConnectorHubBatch<StreamingData<String>> batch) {

    }
}
