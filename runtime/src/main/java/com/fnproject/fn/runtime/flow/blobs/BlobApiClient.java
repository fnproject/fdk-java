package com.fnproject.fn.runtime.flow.blobs;

public interface BlobApiClient {

    BlobDatum writeBlob(String prefix, byte[] bytes, String contentType);
}
