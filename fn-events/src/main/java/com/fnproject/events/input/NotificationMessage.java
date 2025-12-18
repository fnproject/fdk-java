package com.fnproject.events.input;

import java.util.Objects;
import com.fnproject.fn.api.Headers;

public class NotificationMessage<T> {
    private final T content;
    private final Headers headers;

    public NotificationMessage(T content, Headers headers) {
        this.content = content;
        this.headers = headers;
    }

    public T getContent() {
        return content;
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
        NotificationMessage<?> message = (NotificationMessage<?>) o;
        return Objects.equals(content, message.content) && Objects.equals(headers, message.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, headers);
    }

}
