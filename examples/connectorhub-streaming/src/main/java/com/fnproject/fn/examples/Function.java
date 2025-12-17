package com.fnproject.fn.examples;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.StreamingData;

public class Function extends ConnectorHubFunction<StreamingData<Employee>> {

    public StreamService streamService;

    public Function() {
        this.streamService = new StreamService();
    }

    @Override
    public void handler(ConnectorHubBatch<StreamingData<Employee>> batch) {
        for (StreamingData<Employee> stream : batch.getBatch()) {
            streamService.readStream(stream);
        }
    }
}