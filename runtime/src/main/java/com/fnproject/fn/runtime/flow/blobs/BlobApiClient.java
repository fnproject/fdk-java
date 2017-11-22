package com.fnproject.fn.runtime.flow.blobs;

import com.fnproject.fn.runtime.flow.APIModel;

public interface BlobApiClient {

    APIModel.Blob writeBlob(String prefix, byte[] bytes, String contentType);
    BlobResponse readBlob(String prefix, String blobId, String expectedContentType);
}
