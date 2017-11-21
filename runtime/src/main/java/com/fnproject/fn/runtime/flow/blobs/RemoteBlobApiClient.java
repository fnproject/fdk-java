package com.fnproject.fn.runtime.flow.blobs;

public class RemoteBlobApiClient implements BlobApiClient {
    private final String apiUrlBase;

    public RemoteBlobApiClient(String apiUrlBase) {
        this.apiUrlBase = apiUrlBase;
    }

    @Override
    public BlobDatum writeBlob(byte[] bytes, String contentType) {
        return new BlobDatum("blobid", contentType, bytes.length);
    }
}
