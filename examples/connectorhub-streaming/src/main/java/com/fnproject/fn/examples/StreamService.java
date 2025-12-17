package com.fnproject.fn.examples;

import com.fnproject.events.input.sch.StreamingData;

public class StreamService {

    public void readStream(StreamingData<Employee> streamingData) {
        System.out.println(streamingData);
        assert streamingData != null;
        assert streamingData.getStream() != null;
        assert streamingData.getPartition() != null;
        assert streamingData.getValue() != null;
        assert streamingData.getOffset() != null;
        assert streamingData.getTimestamp() != null;

        Employee employee = streamingData.getValue();
        System.out.println(employee);
    }
}
