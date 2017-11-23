package com.fnproject.fn.runtime.flow.blobs;

import com.fnproject.fn.runtime.flow.APIModel;

import java.io.InputStream;
import java.util.function.Function;

public interface BlobApiClient {

    APIModel.Blob writeBlob(String prefix, byte[] bytes, String contentType);
    BlobResponse readBlob(String prefix, String blobId, Function<InputStream, Object> writer, String expectedContentType);
}
