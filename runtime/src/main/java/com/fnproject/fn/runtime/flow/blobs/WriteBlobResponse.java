package com.fnproject.fn.runtime.flow.blobs;

import com.fasterxml.jackson.annotation.JsonProperty;

class WriteBlobResponse {
    @JsonProperty("blob_id")
    String blobId;
    @JsonProperty("content_type")
    String contentType;
    @JsonProperty("length")
    Integer length;
}
