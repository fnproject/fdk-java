package com.fnproject.fn.examples;


import com.fnproject.events.input.sch.LoggingData;

public class LogService {

    public void readLog(LoggingData loggingData) {
        System.out.println(loggingData);
        assert loggingData != null;
        assert loggingData.getData() != null && !loggingData.getData().isEmpty();
        assert loggingData.getId() != null;
        assert loggingData.getOracle() != null && !loggingData.getOracle().isEmpty();;
        assert loggingData.getSource() != null;
        assert loggingData.getSpecversion() != null;
        assert loggingData.getTime() != null;
        assert loggingData.getType() != null;
    }
}
