package com.fnproject.fn.examples;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;
import com.fnproject.fn.api.cloudthreads.HttpMethod;
import io.minio.MinioClient;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

public class ThumbnailsFunction implements Serializable {

    private final String storageUrl;
    private final String storageAccessKey;
    private final String storageSecretKey;

    public ThumbnailsFunction(RuntimeContext ctx) {
        storageUrl = ctx.getConfigurationByKey("OBJECT_STORAGE_URL")
                .orElseThrow(() -> new RuntimeException("Missing configuration: OBJECT_STORAGE_URL"));
        storageAccessKey = ctx.getConfigurationByKey("OBJECT_STORAGE_ACCESS")
                .orElseThrow(() -> new RuntimeException("Missing configuration: OBJECT_STORAGE_ACCESS"));
        storageSecretKey = ctx.getConfigurationByKey("OBJECT_STORAGE_SECRET")
                .orElseThrow(() -> new RuntimeException("Missing configuration: OBJECT_STORAGE_SECRET"));
    }

    public class Response {
        Response(String imageId) { this.imageId = imageId; }
        public String imageId;
    }

    public Response handleRequest(byte[] imageBuffer) {
        String id = java.util.UUID.randomUUID().toString();
        CloudThreadRuntime runtime = CloudThreads.currentRuntime();

        runtime.allOf(
                runtime.invokeFunction("myapp/resize128", HttpMethod.POST, Headers.emptyHeaders(), imageBuffer)
                        .thenAccept((img) -> objectUpload(img.getBodyAsBytes(), id + "-128.png")),
                runtime.invokeFunction("myapp/resize256", HttpMethod.POST, Headers.emptyHeaders(), imageBuffer)
                        .thenAccept((img) -> objectUpload(img.getBodyAsBytes(), id + "-256.png")),
                runtime.invokeFunction("myapp/resize512", HttpMethod.POST, Headers.emptyHeaders(), imageBuffer)
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
            MinioClient minioClient = new MinioClient(storageUrl, storageAccessKey, storageSecretKey);

            // Ensure the bucket exists.
            if(!minioClient.bucketExists("alpha")) {
                minioClient.makeBucket("alpha");
            }

            // Upload the image to the bucket with putObject
            minioClient.putObject("alpha", objectName, new ByteArrayInputStream(imageBuffer), imageBuffer.length, "application/octet-stream");
        } catch(Exception e) {
            System.err.println("Error occurred: " + e);
            e.printStackTrace();
        }
    }
}
