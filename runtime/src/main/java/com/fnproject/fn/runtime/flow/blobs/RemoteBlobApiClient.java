package com.fnproject.fn.runtime.flow.blobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.flow.PlatformException;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;
import com.fnproject.fn.runtime.flow.HttpClient;

import java.io.IOException;

public class RemoteBlobApiClient implements BlobApiClient {
    private final String apiUrlBase;
    private final HttpClient httpClient;

    public RemoteBlobApiClient(String apiUrlBase, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.apiUrlBase = apiUrlBase;
    }

    @Override
    public BlobDatum writeBlob(String prefix, byte[] bytes, String contentType) {
        HttpClient.HttpRequest request = HttpClient.preparePost(apiUrlBase + "/" + prefix)
                .withHeader("Content-Type", contentType)
                .withBody(bytes);
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            if(resp.getStatusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                WriteBlobResponse writeBlobResponse = objectMapper.readValue(resp.getEntity(), WriteBlobResponse.class);
                System.out.println(writeBlobResponse.blobId);
                return new BlobDatum(writeBlobResponse.blobId, writeBlobResponse.contentType, writeBlobResponse.length);
            } else {
                throw new PlatformException("Failed to write blob");
            }
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to write blob", e);
        }
    }
}
