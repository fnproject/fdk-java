package com.fnproject.fn.examples;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;

public class Function extends ConnectorHubFunction<Employee> {

    public QueueService queueService;

    public Function() {
        this.queueService = new QueueService();
    }

    @Override
    public void handler(ConnectorHubBatch<Employee> batch) {
        for (Employee employee : batch.getBatch()) {
            queueService.readContent(employee);
        }
    }
}