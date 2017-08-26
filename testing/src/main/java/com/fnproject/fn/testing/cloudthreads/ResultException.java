package com.fnproject.fn.testing.cloudthreads;

/**
 * Created on 26/08/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class ResultException extends RuntimeException{
    private final Datum datum;


    public ResultException(Datum datum) {
        this.datum = datum;
    }

    public Result toResult(){
        return Result.failure(datum);
    }
}
