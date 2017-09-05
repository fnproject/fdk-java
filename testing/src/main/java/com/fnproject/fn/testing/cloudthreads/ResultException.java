package com.fnproject.fn.testing.cloudthreads;

public class ResultException extends RuntimeException{
    private final Datum datum;

    public ResultException(Datum datum) {
        this.datum = datum;
    }

    public Result toResult(){
        return Result.failure(datum);
    }
}
