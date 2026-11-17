# HTTP server provider SPI

The standard FDK entry point discovers an optional
`com.fnproject.fn.runtime.http.spi.HttpServerProvider` using Java `ServiceLoader`.
This applies to `FN_FORMAT=http-stream`.

* No provider registered: FDK constructs the existing `HTTPStreamCodec`, using
  Apache HttpCore and the JNI with C-server on Unix Domain Socket implementation.
* One provider registered: FDK calls that provider to create the codec.
* Multiple providers, an invalid registration, or a failed provider: startup
  fails. FDK does not silently fall back to the built-in server.

## Implementing a provider

The provider must supply a complete Fn HTTP stream codec, including HTTP-to-event
conversion. A server-neutral HTTP request API and shared Fn protocol adapter are
not part of this change.

```java
public final class CustomHttpServerProvider implements HttpServerProvider {
    public CustomHttpServerProvider() {
    }

    @Override
    public EventCodec create(Map<String, String> environment,
                             String fdkVersion,
                             String runtimeVersion) throws Exception {
        return new CustomHttpStreamCodec(environment, fdkVersion, runtimeVersion);
    }
}
```

`CustomHttpStreamCodec` is an application-supplied implementation of `EventCodec`.
The environment is a read-only snapshot. The version arguments contain the
values to report in `Fn-Fdk-Version` and `Fn-Fdk-Runtime`.

Put the provider's fully qualified class name in this UTF-8 file in its JAR:

```text
META-INF/services/com.fnproject.fn.runtime.http.spi.HttpServerProvider
```

For example, the file contains:

```text
com.acme.fn.CustomHttpServerProvider
```

The provider must have a public no-argument constructor. Discovery uses the
thread context classloader, falling back to the SPI's classloader if the context
classloader is null. Keep construction free of socket binding or thread startup;
FDK checks for multiple providers before calling `create`.

FDK calls `runCodec` with its existing function handler. The codec must invoke
that handler serially, implement the Fn HTTP stream protocol over `FN_LISTENER`,
and release its resources when its event loop exits. The existing entry point
does not call `AutoCloseable.close` on codecs. No new shutdown contract is added.
Function initialization stays lazy, and function APIs are unchanged.

Provider dependencies and the service descriptor must be visible on the runtime
classpath. Merge service descriptors when building a shaded JAR. Do not bundle
a second copy of FDK runtime classes. Removing the service registration and
restarting restores the built-in server.
