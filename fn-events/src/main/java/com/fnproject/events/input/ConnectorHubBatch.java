package com.fnproject.events.input;

import java.util.List;
import java.util.Objects;
import com.fnproject.fn.api.Headers;

public class ConnectorHubBatch<T> {
    private final Headers headers;
    private final List<T> batch;

    public ConnectorHubBatch(List<T> batch, Headers headers) {
        this.batch = batch;
        this.headers = headers;
    }

    public List<T> getBatch() {
        return batch;
    }

    public Headers getHeaders() {
        return headers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectorHubBatch<?> that = (ConnectorHubBatch<?>) o;
        return Objects.equals(headers, that.headers) && Objects.equals(batch, that.batch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers, batch);
    }

}
