# Testing Fn Events

You can use `FnEventTesting` to test [README.md](../fn-events/README.md) within your functions. 

Start by importing the `fn-events-testing` library into your function in `test` scope:

```xml
<dependency>
    <groupId>com.fnproject.fn</groupId>
    <artifactId>fn-events-testing</artifactId>
    <version>${fdk.version}</version>
    <scope>test</scope>
</dependency>
```

## Usage
- OCI API Gateway Function - [README.md](../examples/apigateway-event/README.md)
- OCI Service Connector Hub: Monitoring - [README.md](../examples/connectorhub-monitoring/README.md)
- OCI Service Connector Hub: Logging - [README.md](../examples/connectorhub-logging/README.md)
- OCI Service Connector Hub: Streaming - [README.md](../examples/connectorhub-streaming/README.md)
- OCI Service Connector Hub: Queue - [connectorhub-queue](../examples/connectorhub-queue)
- OCI Notifications - [README.md](../examples/notifications/README.md)
