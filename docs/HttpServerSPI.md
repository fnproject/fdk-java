# HTTP server provider SPI

The standard FDK entry point discovers an optional
`com.fnproject.fn.runtime.http.spi.HttpServerProvider` using Java `ServiceLoader`.
This applies to `FN_FORMAT=http-stream`.

* No provider registered: FDK uses `HTTPStreamCodec`, Apache HttpCore, and the
  existing JNI/C Unix socket transport.
* One provider registered: FDK creates its `HttpServer` and connects it to the
  shared Fn protocol handler through `HttpServerCodec`.
* Multiple providers, invalid registration, or provider failure: startup fails.
  FDK does not silently fall back to the built-in server.

`FN_FORMAT` and `FN_LISTENER` retain their existing meanings. No environment
variable selects the provider. The built-in server is a direct fallback rather
than a registered service.

## What FDK handles

Both the built-in server and custom providers use `FnHttpHandler`. It owns:

* `/call` routing, including the existing query-string handling. Unknown paths
  return 501, matching the built-in server. The adapter adds no method restriction.
* Required `Fn-Call-Id` and `Fn-Deadline` headers, deadline parsing, and conversion
  to a read-once `InputEvent`.
* Input and output header filtering, repeated values, output status mapping,
  content type handling, and generated FDK version headers.
* Existing JSON error responses for invalid input, invocation errors, and output
  serialization errors.
* Serial execution of request conversion, the function handler, and output
  serialization. The complete output is buffered before the callback returns.

Providers do not implement `EventCodec` or use Fn event types. The shared handler
uses the existing runtime for lazy function initialization and invocation.

## Implementing and registering a provider

Implement the SPI with a public no-argument constructor:

```java
import com.fnproject.fn.runtime.http.spi.HttpServer;
import com.fnproject.fn.runtime.http.spi.HttpServerProvider;
import java.util.Map;

public final class CustomHttpServerProvider implements HttpServerProvider {
    public CustomHttpServerProvider() {
    }

    @Override
    public HttpServer create(Map<String, String> environment) throws Exception {
        return new CustomHttpServer(environment);
    }
}
```

`CustomHttpServer` is the integration you build for your chosen HTTP engine.
The environment is a read-only snapshot. Return a non-null server and release
any partial resources if creation fails. Do not handle requests until FDK calls
`run(HttpHandler)`. The version strings stay inside FDK and are no longer passed
to providers.

The supplied `HttpHandler` uses `HttpServerRequest` and `HttpServerResponse`.
Wrap your HTTP engine's request and response objects with these interfaces.

Put this file in the provider JAR:

```text
META-INF/services/com.fnproject.fn.runtime.http.spi.HttpServerProvider
```

Its UTF-8 content is the provider class name, for example:

```text
com.acme.fn.CustomHttpServerProvider
```

Discovery uses the thread context classloader, falling back to the SPI classloader
when it is `null`. Keep the provider constructor free of socket binding and worker
startup, because discovery checks for multiple providers before calling `create`.
