package com.fnproject.fn.runtime.flow.blobs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BlobDatum {
    public BlobDatum(String blobId, String contentType, Integer length) {
        this.blobId = blobId;
        this.contentType = contentType;
        this.length = length;
    }

    @JsonProperty("blob_id")
    String blobId;
    @JsonProperty("content_type")
    String contentType;
    @JsonProperty("length")
    Integer length;
}
