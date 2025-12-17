package com.fnproject.fn.examples;

import com.fnproject.events.ConnectorHubFunction;
import com.fnproject.events.input.ConnectorHubBatch;
import com.fnproject.events.input.sch.LoggingData;

public class Function extends ConnectorHubFunction<LoggingData> {

    public LogService logService;

    public Function() {
        this.logService = new LogService();
    }

    @Override
    public void handler(ConnectorHubBatch<LoggingData> batch) {
        for (LoggingData log : batch.getBatch()) {
            logService.readLog(log);
        }
    }
}