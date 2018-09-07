package com.fnproject.fn.runtime.flow;

import java.io.InputStream;
import java.util.function.Function;

public interface BlobStoreClient {


    BlobResponse writeBlob(String prefix, byte[] bytes, String contentType);

    <T> T readBlob(String prefix, String blobId, Function<InputStream, T> reader, String expectedContentType);
}
