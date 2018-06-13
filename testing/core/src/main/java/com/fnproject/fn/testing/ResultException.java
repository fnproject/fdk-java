package com.fnproject.fn.testing;

import com.fnproject.fn.runtime.flow.APIModel;

class ResultException extends RuntimeException {
    private final APIModel.Datum datum;

    ResultException(APIModel.Datum datum) {
        this.datum = datum;
    }

    APIModel.CompletionResult toResult() {
        return APIModel.CompletionResult.failure(datum);
    }
}
