package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public class RemoteBlobStoreClient implements BlobStoreClient {
    private final String apiUrlBase;
    private final HttpClient httpClient;

    public RemoteBlobStoreClient(String apiUrlBase, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.apiUrlBase = apiUrlBase;
    }

    @Override
    public BlobResponse writeBlob(String prefix, byte[] bytes, String contentType) {
        HttpClient.HttpRequest request = HttpClient.preparePost(apiUrlBase + "/" + prefix)
           .withHeader("Content-Type", contentType)
           .withBody(bytes);
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            if (resp.getStatusCode() == 200) {
                return FlowRuntimeGlobals.getObjectMapper().readValue(resp.getEntity(), BlobResponse.class);
            } else {
                throw new PlatformCommunicationException("Failed to write blob, got non 200 response:" + resp.getStatusCode() + " from blob store");
            }
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to write blob", e);
        }
    }

    @Override
    public <T> T readBlob(String prefix, String blobId, Function<InputStream, T> writer, String expectedContentType) {
        HttpClient.HttpRequest request = HttpClient.prepareGet(apiUrlBase + "/" + prefix + "/" + blobId).withHeader("Accept", expectedContentType);
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            if (resp.getStatusCode() == 200) {
                return writer.apply(resp.getContentStream());
            } else {
                throw new PlatformCommunicationException("Failed to read blob, got non-200 status : " + resp.getStatusCode() + " from blob store");
            }
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to read blob", e);
        }
    }
}
