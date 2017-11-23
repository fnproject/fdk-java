package com.fnproject.fn.runtime.flow.blobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.flow.PlatformException;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;
import com.fnproject.fn.runtime.flow.APIModel;
import com.fnproject.fn.runtime.flow.HttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public class RemoteBlobApiClient implements BlobApiClient {
    private final String apiUrlBase;
    private final HttpClient httpClient;

    public RemoteBlobApiClient(String apiUrlBase, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.apiUrlBase = apiUrlBase;
    }

    @Override
    public APIModel.Blob writeBlob(String prefix, byte[] bytes, String contentType) {
        HttpClient.HttpRequest request = HttpClient.preparePost(apiUrlBase + "/" + prefix)
           .withHeader("Content-Type", contentType)
           .withBody(bytes);
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            if (resp.getStatusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                APIModel.Blob writeBlobResponse = objectMapper.readValue(resp.getEntity(), APIModel.Blob.class);
                System.out.println(writeBlobResponse.blobId);
                return writeBlobResponse;
            } else {
                throw new PlatformException("Failed to write blob");
            }
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to write blob", e);
        }
    }

    @Override
    public BlobResponse readBlob(String prefix, String blobId, Function<InputStream, Object> writer, String expectedContentType) {
        HttpClient.HttpRequest request = HttpClient.prepareGet(apiUrlBase + "/" + prefix + "/" + blobId).withHeader("Accept", expectedContentType);
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            if (resp.getStatusCode() == 200) {
                BlobResponse blobResponse = new BlobResponse();
                blobResponse.data = writer.apply(resp.getEntity());
                blobResponse.contentType = resp.getHeaderValue("Content-type").orElse("application/octet-stream");
                blobResponse.length = Integer.parseInt(resp.getHeaderValue("Content-length").orElse("0"));
                return blobResponse;
            } else {
                throw new PlatformException("Failed to read blob");
            }
        } catch (IOException e) {
            throw new PlatformCommunicationException("Failed to read blob", e);
        }
    }
}
