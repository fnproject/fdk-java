package com.fnproject.fn.runtime.flow;

import java.io.Serializable;

public interface CompleterClientFactory extends Serializable {
    CompleterClient getCompleterClient();

    BlobStoreClient getBlobStoreClient();

}
