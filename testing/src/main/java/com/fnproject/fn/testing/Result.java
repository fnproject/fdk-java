package com.fnproject.fn.testing;

import com.fnproject.fn.runtime.cloudthreads.CloudCompleterApiClient;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

class Result {

    private final boolean success;
    private final Datum datum;

    Result(boolean success, Datum datum) {
        this.success = success;
        this.datum = Objects.requireNonNull(datum);
    }

    Datum getDatum() {
        return datum;
    }

    static Result success(Datum d) {
        return new Result(true, d);

    }

    static Result failure(Datum d) {
        return new Result(false, d);
    }

    Result toEmpty() {
        return new Result(success, new Datum.EmptyDatum());
    }

    Object toJavaObject(ClassLoader loader) {
        return datum.asJavaValue(loader);
    }


    void writePart(OutputStream os) throws IOException {
        HeaderWriter hw = new HeaderWriter(os);

        hw.writeHeader(CloudCompleterApiClient.RESULT_STATUS_HEADER, success ? CloudCompleterApiClient.RESULT_STATUS_SUCCESS : CloudCompleterApiClient.RESULT_STATUS_FAILURE);

        datum.writeHeaders(hw);
        os.write(new byte[]{'\r', '\n'});
        datum.writeBody(os);
    }

    static Result readResult(HttpResponse response) throws IOException {
        String status = response.getFirstHeader(CloudCompleterApiClient.RESULT_STATUS_HEADER).getValue();

        boolean success;
        switch (status) {
            case CloudCompleterApiClient.RESULT_STATUS_SUCCESS:
                success = true;
                break;
            case CloudCompleterApiClient.RESULT_STATUS_FAILURE:
                success = false;
                break;
            default:
                throw new RuntimeException("Invalid status");

        }
        String datumTypeString = response.getFirstHeader(CloudCompleterApiClient.DATUM_TYPE_HEADER).getValue();

        Datum.DatumType datumType = Datum.DatumType.valueOf(datumTypeString);

        return new Result(success, datumType.reader.readDatum(response));
    }


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Result{");
        sb.append("success=").append(success);
        sb.append(", datum=").append(datum);
        sb.append('}');
        return sb.toString();
    }

    boolean isSuccess() {
        return success;
    }
}
