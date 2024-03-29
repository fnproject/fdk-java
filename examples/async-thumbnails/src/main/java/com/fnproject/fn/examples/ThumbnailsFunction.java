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

package com.fnproject.fn.examples;

import com.fnproject.fn.api.FnFeature;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.runtime.flow.FlowFeature;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.api.flow.HttpMethod;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

@FnFeature(FlowFeature.class)
public class ThumbnailsFunction implements Serializable {

    private final String storageUrl;
    private final String storageAccessKey;
    private final String storageSecretKey;

    private final String resize128ID;
    private final String resize256ID;
    private final String resize512ID;

    public ThumbnailsFunction(RuntimeContext ctx) {
        storageUrl = ctx.getConfigurationByKey("OBJECT_STORAGE_URL")
                .orElseThrow(() -> new RuntimeException("Missing configuration: OBJECT_STORAGE_URL"));
        storageAccessKey = ctx.getConfigurationByKey("OBJECT_STORAGE_ACCESS")
                .orElseThrow(() -> new RuntimeException("Missing configuration: OBJECT_STORAGE_ACCESS"));
        storageSecretKey = ctx.getConfigurationByKey("OBJECT_STORAGE_SECRET")
                .orElseThrow(() -> new RuntimeException("Missing configuration: OBJECT_STORAGE_SECRET"));

        resize128ID = ctx.getConfigurationByKey("RESIZE_128_FN_ID")
          .orElseThrow(() -> new RuntimeException("Missing configuration: RESIZE_128_FN_ID"));
        resize256ID = ctx.getConfigurationByKey("RESIZE_256_FN_ID")
          .orElseThrow(() -> new RuntimeException("Missing configuration: RESIZE_256_FN_ID"));
        resize512ID = ctx.getConfigurationByKey("RESIZE_512_FN_ID")
          .orElseThrow(() -> new RuntimeException("Missing configuration: RESIZE_512_FN_ID"));

    }

    public class Response {
        Response(String imageId) { this.imageId = imageId; }
        public String imageId;
    }

    public Response handleRequest(byte[] imageBuffer) {
        String id = java.util.UUID.randomUUID().toString();
        Flow runtime = Flows.currentFlow();

        runtime.allOf(
                runtime.invokeFunction(resize128ID, HttpMethod.POST, Headers.emptyHeaders(), imageBuffer)
                        .thenAccept((img) -> objectUpload(img.getBodyAsBytes(), id + "-128.png")),
                runtime.invokeFunction(resize256ID, HttpMethod.POST, Headers.emptyHeaders(), imageBuffer)
                        .thenAccept((img) -> objectUpload(img.getBodyAsBytes(), id + "-256.png")),
                runtime.invokeFunction(resize512ID, HttpMethod.POST, Headers.emptyHeaders(), imageBuffer)
                        .thenAccept((img) -> objectUpload(img.getBodyAsBytes(), id + "-512.png")),
                runtime.supply(() -> objectUpload(imageBuffer, id + ".png"))
        );
        return new Response(id);
    }

    /**
     * Uploads the provided data to the storage server, as an object named as specified.
     *
     * @param imageBuffer the image data to upload
     * @param objectName the name of the remote object to create
     */
    private void objectUpload(byte[] imageBuffer, String objectName) {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(storageUrl).credentials(storageAccessKey, storageSecretKey).build();

            // Ensure the bucket exists.
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket("alpha").build();
            MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket("alpha").build();
            if(!minioClient.bucketExists(bucketExistsArgs)) {
                minioClient.makeBucket(makeBucketArgs);
            }

            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket("alpha")
                    .object(objectName)
                    .stream(new ByteArrayInputStream(imageBuffer), imageBuffer.length, -1).build();
            // Upload the image to the bucket with putObject
            minioClient.putObject(putObjectArgs);
        } catch(Exception e) {
            System.err.println("Error occurred: " + e);
            e.printStackTrace();
        }
    }
}
