# Example fn Java FDK / Cloud Threads Project: Asynchronous Thumbnails

This example provides an HTTP endpoint for asynchronously creating three
thumbnails of an image whose data is provided as the body of the HTTP request
as an octet stream. The thumbnails are then transferred to an object storage
server, credentials for which are configured in the function configuration.
The function returns a string unique ID which corresponds to the image.

The intention is that such a function would be used in the context of a wider
system which then would store a correspondence between that ID and further data.
For example, the thumbnailed image might be a user's avatar in a web app, and
its ID would be linked to the user's ID in a database somewhere.


## Demonstrated FDK features

This example showcases how to use the Cloud Threads API to invoke other
*fn functions* asynchronously in a workflow expressed in code.


## Step by step

A `setup.sh` script is provided to set up the environment you will need for
this example. Run:

```bash
./setup/setup.sh
```

This will start a local functions service, a local cloud threads completion
service, and will set up a `myapp` application and three routes: `/resize128`,
`/resize256` and `/resize512`. The routes are implemented as *fn functions*
which just invoke `imagemagick` to convert the images to the specified sizes.

The setup script also starts a docker container with an object storage daemon
based on `minio` (with access key `alpha` and secret key `betabetabetabeta`).

The local directories `./storage-upload` is mapped as a volume in the
docker container, so that you can verify when the thumbnails are uploaded.

Build the function locally:

```bash
$ fn build
```

Create a route to host the function:

```bash
$ fn routes create myapp /async-thumbnails
```

Configure the app. In order to do this you must determine the IP address of the
storage server docker container:

```bash
$ docker inspect --type container -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' example-storage-server
172.17.0.4
```

and then use it as the storage host:

```bash
$ fn routes config set myapp /async-thumbnails OBJECT_STORAGE_URL http://172.17.0.4
myapp /async-thumbnails updated OBJECT_STORAGE_URL with http://172.17.0.4
$ fn routes config set myapp /async-thumbnails OBJECT_STORAGE_ACCESS alpha
myapp /async-thumbnails updated OBJECT_STORAGE_ACCESS with alpha
$ fn routes config set myapp /async-thumbnails OBJECT_STORAGE_SECRET betabetabetabeta
myapp /async-thumbnails updated OBJECT_STORAGE_SECRET with betabetabetabeta
```

Invoke the function by passing the provided test image:

```bash
$ curl -X POST --data-binary @test-image.png -H "Content-type: application/octet-stream" "http://localhost:8080/r/myapp/async-thumbnails"
{"imageId":"bd74fff4-0388-4c6f-82f2-8cde9ba9b6fc"}
```

After a while, you will find the files in `./storage-upload/alpha/`:

```bash
$ ls storage-upload/alpha/
bd74fff4-0388-4c6f-82f2-8cde9ba9b6fc-128.png    bd74fff4-0388-4c6f-82f2-8cde9ba9b6fc-512.png
bd74fff4-0388-4c6f-82f2-8cde9ba9b6fc-256.png    bd74fff4-0388-4c6f-82f2-8cde9ba9b6fc.png
```


## Code walkthrough

The entrypoint to the function is specified in `func.yaml` in the `cmd` key.
It is set to `com.fnproject.fn.examples.ThumbnailsFunction::handleRequest`.

The class `ThumbnailsFunction` has a constructor [which reads configuration data](../../docs/FunctionConfiguration.md)
to obtain the details of the storage server host, username and password:

```java
public class ThumbnailsFunction {

    private final String storageHost;
    private final String storageAccessKey;
    private final String storageSecretKey;

    public ThumbnailsFunction(RuntimeContext ctx) {
        storageHost = ctx.getConfigurationByKey("OBJECT_STORAGE_URL");
        storageAccessKey = ctx.getConfigurationByKey("OBJECT_STORAGE_ACCESS");
        storageSecretKey = ctx.getConfigurationByKey("OBJECT_STORAGE_SECRET");
    }

    // ...
}
```

It also features an inner `Response` class which is used as a POJO return type, which the runtime will serialize to
JSON. See [Data Binding](../../docs/DataBinding.md) for more information on how marshalling is performed.

```java
public class ThumbnailsFunction {

    // ...

    public class Response {
        Response(String imageId) { this.imageId = imageId; }
        String imageId;
    }

    // ...
}
```

The main method of the function makes use of the Cloud Threads API, spawning three concurrent asynchronous tasks, each
of which performs the creation of one thumbnail and its upload to the object storage server. The original image is also
uploaded alongside the thumbnails.

The file names are generated from a UUID which is then returned by the function in the response.

```java
public class ThumbnailsFunction {

    // ...

    public Response handleRequest(byte[] imageBuffer) {
        String id = java.util.UUID.randomUUID().toString();
        CloudThreadRuntime runtime = CloudThreads.currentRuntime();
        runtime.allOf(
                runtime.invokeFunction("myapp/resize128", imageBuffer)
                        .thenAccept((img) -> objectUpload(img, id + "-128.png")),
                runtime.invokeFunction("myapp/resize256", imageBuffer)
                        .thenAccept((img) -> objectUpload(img, id + "-256.png")),
                runtime.invokeFunction("myapp/resize512", imageBuffer)
                        .thenAccept((img) -> objectUpload(img, id + "-512.png")),
                runtime.supply(() -> objectUpload(imageBuffer, id + ".png"))
        );
        return new Response(id);
    }

    // ...
}
```

The `objectUpload` method is private and just uses a `minio` client to perform the upload.

```java
public class ThumbnailsFunction {

    // ...

    /**
     * Uploads the provided data to the storage server, as an object named as specified.
     *
     * @param imageBuffer the image data to upload
     * @param objectName the name of the remote object to create
     */
    private void objectUpload(byte[] imageBuffer, String objectName) {
        try {
            MinioClient minioClient = new MinioClient(storageHost, 9000, storageAccessKey, storageSecretKey);

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

    // ...
}
```
