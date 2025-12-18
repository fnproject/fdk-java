# Support tools for functions OCI integrations

This is an optional module that can be added to Functions that resolves some common integrations related to
OCI Services handling oracle functions invocations.

* OCI API Gateway support

# Enabling the feature in Function:

Edit your pom file and add this library as a dependency:

```xml
<dependency>
    <groupId>com.fnproject.fn</groupId>
    <artifactId>fn-events</artifactId>
    <version>${fdk.version}</version>
</dependency>
```

Optionally, add the fn-events-testing library:

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
