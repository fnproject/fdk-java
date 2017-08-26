package com.fnproject.fn.testing.cloudthreads;

import java.util.Objects;

/**
 * Created on 26/08/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class Result {
    private final boolean success;
    private final Datum datum;

    Result(boolean success, Datum datum) {
        this.success = success;
        this.datum = Objects.requireNonNull(datum);
    }

    public boolean isSuccess() {
        return success;
    }

    public Datum getDatum() {
        return datum;
    }

    public static Result success(Datum d) {
        return new Result(true, d);

    }

    public static Result failure(Datum d) {
        return new Result(false, d);
    }

    public  Result toEmpty() {
        return new Result(success,new  Datum.EmptyDatum());
    }

    public Object toJavaObject() {
        return datum.toJava(success);
    }
}
