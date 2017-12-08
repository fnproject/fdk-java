# Fn Java Function Developement Kit (FDK) FAQ

## General

### What is the Fn Java FDK?
The Fn Java FDK helps you build, test and run Java functions on the Fn platform.

The FDK is comprised of:
   - a Java library (known as the `api`);
   - a runtime Docker image (known as the `runtime`);
   - a JUnit rule;
   - a build-time Docker image for repeatable builds. 

### Is the FDK required in order to run Java on Fn?
No. You can still write Java functions on Fn without using the FDK. However using the FDK will make several things easier for you:
   1. A curated base image for Java 8 and Java 9 means that you don't have to build and maintain your own image. These images contain optimizations for quick JVM startup times.
   1. Accessing configuration from Fn is easy through FDK APIs.
   1. Input and output type coercion reduces the amount of serialization and formatting boilerplate that you have to write.
   1. A JUnit rule provides a realistic test harness for you to test your function in isolation.

### What is Fn Flow?
Fn Flow is a [Java API](https://github.com/fnproject/fn-java-fdk/blob/master/docs/FnFlowsUserGuide.md) and [corresponding service](https://github.com/fnproject/flow) that helps you create complex, long-running, fault-tolerant functions using a promises-style asynchronous API. Check out the [Fn Flow docs](https://github.com/fnproject/fn-java-fdk/blob/master/docs/FnFlowsUserGuide.md) for more information.

### How do I get the FDK?
The FDK is automatically added to your project if you built your function using `fn init --runtime=java`. The `api` and `testing` JARs are published on [our bintray](https://bintray.com/fnproject/fnproject) and the `runtime` is published in our [Docker hub repository](https://hub.docker.com/r/fnproject/fn-java-fdk/).

### How do I add the FDK to an existing project?
   1. Find the latest release from the [releases page](https://github.com/fnproject/fn-java-fdk/releases). For example `1.0.32`.
   1. The FDK JAR is published on [Bintray](https://bintray.com/fnproject/fnproject). Add the repository to your`pom.xml` `repositories` section:
   ```xml
        <repository>
            <id>fn-release-repo</id>
            <url>https://dl.bintray.com/fnproject/fnproject</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
   ```
   1. Add the dependency to your `dependency` section. Make sure that the `version` tag matches the latest release that you looked up above.
   ```xml
        <dependency>
            <groupId>com.fnproject.fn</groupId>
            <artifactId>api</artifactId>
            <version>1.0.32</version>
        </dependency>
   ```
   1. If you want the JUnit support also add:
   ```xml
        <dependency>
            <groupId>com.fnproject.fn</groupId>
            <artifactId>testing</artifactId>
            <version>1.0.32</version>
            <scope>test</scope>
        </dependency>
   ```

### What is the latest version of the FDK?
Have a look on our [releases page](https://github.com/fnproject/fn-java-fdk/releases).

### How do I upgrade to the latest version of the FDK?
   1. Find the latest version from our [releases page](https://github.com/fnproject/fn-java-fdk/releases).
   1. Update the Java `api` version: change your dependency management system to reference the latest artifact. For example to upgrade a maven build to use `1.0.32`:
   ```xml
        <dependency>
            <groupId>com.fnproject.fn</groupId>
            <artifactId>api</artifactId>
            <version>1.0.32</version>
        </dependency>
        <dependency>
            <groupId>com.fnproject.fn</groupId>
            <artifactId>testing</artifactId>
            <version>1.0.32</version>
            <scope>test</scope>
        </dependency>
   ```
   1. Update your runtime image:
      1. To latest: 
      ```sh
      docker pull fnproject/fn-java-fdk
      ```
      
      1. To a specific version: 
      ```
      docker pull fnproject/fn-java-fdk:1.0.32
      docker tag fnproject/fn-java-fdk:1.0.32 fnproject/fn-java-fdk:latest
      ```
   
   You should keep versions of `com.fnproject.fn.api`, `com.fnproject.fn.testing` and your runtime Docker image in sync with each other.

   1. Update your build image:
   ```sh
   docker pull fnproject/fn-java-fdk-build
   ```
   
### I think I found a bug - how do I report it?
Please create an [issue on our GitHub repo](https://github.com/fnproject/fn-java-fdk/issues).

### My question is not answered here - how do I get more help?
We hang out on the [Fn project slack](https://join.slack.com/t/fnproject/shared_invite/enQtMjIwNzc5MTE4ODg3LTdlYjE2YzU1MjAxODNhNGUzOGNhMmU2OTNhZmEwOTcxZDQxNGJiZmFiMzNiMTk0NjU2NTIxZGEyNjI0YmY4NTA) in #fn-flow, #fn-java-fdk.

## Fn Flow

### How do I write a function that takes advantage of the Fn Flow API?
Please see our [Fn Flow User Guide](https://github.com/fnproject/fn-java-fdk/blob/master/docs/FnFlowsUserGuide.md) for information on how to get started with Fn Flow in Java.

### When should I use Fn Flow?
Use Fn Flow when:
   - You want to compose the work of several other Fn functions in a reliable and scalable way.
   - You find yourself wanting to write code that blocks on the result of another Fn call.
   - You feel tempted to reach for another workflow system or library.

### Is Fn Flow related to `java.util.concurrent.Flow`?
No. `java.util.concurrent.Flow` is a stream-processing API for running code in a single JVM. Fn Flow is a distributed promise API for writing long-running, fault-tolerant asynchronous functions.

### Is Fn Flow related to <any other Java library called Flow>?
No. Fn Flow was released in 2017 and is not related to any other Java library.

### What is the relationship between Fn Flow and Java's `CompletionStage` and `CompletableFuture` API?
Fn Flow was 'inspired by' the `CompletionStage` API and shares a number of similar methods. However, as the semantics of Fn Flow are subtly different, and because we require our operations to implement `Serializable` we have implemented a new API.

### Can I run my own Fn Flow server?
Fn Flow is open source just like the rest of the Fn project. You can [get the code](https://github.com/fnproject/flow) and [run a server](https://github.com/fnproject/flow#running-the-flow-service) easily.

## Troubleshooting

### TBD
