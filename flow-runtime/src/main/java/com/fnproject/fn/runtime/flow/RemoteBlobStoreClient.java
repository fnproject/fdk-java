/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fnproject.fn.runtime.flow;

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
