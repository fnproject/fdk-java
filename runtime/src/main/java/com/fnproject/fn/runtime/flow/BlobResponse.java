package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BlobResponse {
    @JsonProperty("blob_id")
    public String blobId;

    @JsonProperty("length")
    public Long blobLength;

    @JsonProperty("content_type")
    public String contentType;
}
