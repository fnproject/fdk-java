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

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.fnproject.fn.runtime.flow.RemoteFlowApiClient.CONTENT_TYPE_JAVA_OBJECT;

/**
 * Created on 26/11/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class TestBlobStore implements BlobStoreClient {

    Map<String, byte[]> blobs = new HashMap<>();
    AtomicInteger blobCount = new AtomicInteger();

    @Override
    public BlobResponse writeBlob(String prefix, byte[] bytes, String contentType) {

        String blobId = prefix + "-" + blobCount.incrementAndGet();
        blobs.put(blobId, bytes);
        BlobResponse blob = new BlobResponse();
        blob.contentType = contentType;
        blob.blobLength = (long) bytes.length;
        blob.blobId = blobId;
        return blob;
    }

    @Override
    public <T> T readBlob(String prefix, String blobId, Function<InputStream, T> reader, String expectedContentType) {
        if (!blobs.containsKey(blobId)) {
            throw new IllegalStateException("Blob not found");
        }
        if (!blobId.startsWith(prefix)) {
            throw new IllegalStateException("Invalid blob ID = prefix '" + prefix + "' does not match blob '" + blobId + "'");
        }
        return reader.apply(new ByteArrayInputStream(blobs.get(blobId)));
    }


    public APIModel.Blob withJavaBlob(String prefix, Object o) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
            oos.close();
            BlobResponse br = writeBlob(prefix, bos.toByteArray(), CONTENT_TYPE_JAVA_OBJECT);
            return APIModel.Blob.fromBlobResponse(br);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public APIModel.CompletionResult withResult(String prefix, Object o, boolean status) {
        APIModel.CompletionResult result = new APIModel.CompletionResult();
        result.successful = status;
        APIModel.BlobDatum blobDatum = new APIModel.BlobDatum();
        blobDatum.blob = withJavaBlob(prefix, o);
        result.result = blobDatum;
        return result;

    }

    public <T> T deserializeBlobResult(APIModel.CompletionResult result, Class<T> type) {
        if (!(result.result instanceof APIModel.BlobDatum)) {
            throw new IllegalArgumentException("Datum is not a blob");
        }

        byte[] blob = blobs.get(((APIModel.BlobDatum) result.result).blob.blobId);
        try {
            ObjectInputStream oos = new ObjectInputStream(new ByteArrayInputStream(blob));
            return type.cast(oos.readObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
