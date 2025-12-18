package com.fnproject.events.testfns.connectorhub;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.testfns.Animal;

public class QueueSourceObjectTestFunction extends ConnectorHubFunction<Animal> {

    @Override
    public void handler(ConnectorHubBatch<Animal> batch) {

    }
}
