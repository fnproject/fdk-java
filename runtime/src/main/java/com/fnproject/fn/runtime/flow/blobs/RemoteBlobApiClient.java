package com.fnproject.fn.runtime.flow.blobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.flow.PlatformException;
import com.fnproject.fn.runtime.exception.PlatformCommunicationException;
import com.fnproject.fn.runtime.flow.APIModel;
import com.fnproject.fn.runtime.flow.HttpClient;

import java.io.IOException;
import java.io.ObjectInputStream;

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
    public BlobResponse readBlob(String prefix, String blobId, String expectedContentType) {
        HttpClient.HttpRequest request = HttpClient.prepareGet(apiUrlBase + "/" + prefix + "/" + blobId).withHeader("Accept", expectedContentType);
        try (HttpClient.HttpResponse resp = httpClient.execute(request)) {
            if (resp.getStatusCode() == 200) {
                BlobResponse blobResponse = new BlobResponse();
                // Shouldn't be reading a Java object here, as the content could be anything
                // but the stream is closed if we just return it
                try (ObjectInputStream ois = new ObjectInputStream(resp.getEntity())) {
                    blobResponse.data = ois.readObject();
                } catch (ClassNotFoundException e) {
                    throw new PlatformException("Failed to deserialize blob");
                }
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
