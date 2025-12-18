package com.fnproject.events.testfns.connectorhub;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.StreamingData;
import com.fnproject.events.testfns.Animal;

public class StreamingSourceObjectTestFunction extends ConnectorHubFunction<StreamingData<Animal>> {

    @Override
    public void handler(ConnectorHubBatch<StreamingData<Animal>> batch) {

    }
}
