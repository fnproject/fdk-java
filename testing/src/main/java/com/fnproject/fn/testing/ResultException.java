package com.fnproject.fn.testing;

class ResultException extends RuntimeException {
    private final Datum datum;

    ResultException(Datum datum) {
        this.datum = datum;
    }

    Result toResult() {
        return Result.failure(datum);
    }
}
