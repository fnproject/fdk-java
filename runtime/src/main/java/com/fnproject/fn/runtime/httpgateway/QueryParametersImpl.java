package com.fnproject.fn.runtime.httpgateway;

import com.fnproject.fn.api.QueryParameters;

import java.io.Serializable;
import java.util.*;

public class QueryParametersImpl implements QueryParameters, Serializable {
    private final Map<String, List<String>> params;

    public QueryParametersImpl() {
        this.params = new HashMap<>();
    }

    public QueryParametersImpl(Map<String, List<String>> params) {
        this.params = Objects.requireNonNull(params);
    }

    public Optional<String> get(String key) {
        Objects.requireNonNull(key);
        return Optional.of(getValues(key))
          .filter((values) -> values.size() > 0)
          .flatMap((values) -> Optional.ofNullable(values.get(0)));
    }

    public List<String> getValues(String key) {
        Objects.requireNonNull(key);
        List<String> values = this.params.get(key);
        if (values == null) {
            return Collections.emptyList();
        }
        return values;
    }

    public int size() {
        return params.size();
    }

    @Override
    public Map<String, List<String>> getAll() {
        return new HashMap<>(params);
    }
}
