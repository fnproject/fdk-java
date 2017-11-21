package com.fnproject.fn.runtime.flow.blobs;

public interface BlobApiClient {

    BlobDatum writeBlob(byte[] bytes, String contentType);
}
