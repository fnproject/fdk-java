# Cloud Threads for Fn Java FDK - User Guide

By following this step-by-step guide you will learn to create, run and deploy a simple Java app in Fn that leverages the Cloud Threads asynchronous execution APIs.


## What are Cloud Threads?

Cloud Threads consists of a set of client-side APIs for you to use within your Fn apps, as well as a long-running server component (the _completer_) that orchestrates computation beyond the life-cycle of your functions. Together, these components enable non-blocking asynchronous execution flows, where your function only runs when it has useful work to perform. If you have been exposed to the Java 8 [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) and [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) APIs, a lot of the concepts will already be familiar to you.

## Pre-requisites
Before you get started, you will need to be familiar with the [Fn Java FDK](../README.md) and have the following things:

* [Fn CLI](https://github.com/fnproject/cli)
* [Fn Java FDK](https://github.com/fnproject/fn-java-sdk)
* [Fn completer](https://github.com/fnproject/completer)
* [Docker-ce 17.06+ installed locally](https://docs.docker.com/engine/installation/)
* A [Docker Hub](http://hub.docker.com) account

### Install the Fn CLI tool

To install the Fn CLI tool, just run the following:

curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh

This will download a shell script and execute it. If the script asks for a password, that is because it invokes sudo.

### Log in to DockerHub

You will also need to be logged in to your Docker Hub account in order to deploy functions.

```
docker login
```

### Start a local Fn server and completer server

In a terminal, start the functions server:

```
$ fn start
```

Similarly, start the Cloud Threads completer server by running its docker image locally and linking it to the functions container:

```
$ docker run -p 8081:8081 -d --name completer --link=functions -e API_URL=http://functions:8080/r -e NO_PROXY=functions fnproject/completer:latest
```


## Your first Cloud Thread

### 1. Create your first Cloud Thread application

Create a Maven-based Java Function using the instructions from the Fn Java FDK [Quickstart](../README.md). For example:

```
$ mkdir example-cloudthreads-function && cd example-cloudthreads-function
$ fn init --runtime=java your_dockerhub_account/cloudthread-primes
Runtime: java
function boilerplate generated.
func.yaml created

```

### 2. Create a Cloud Thread within your Function

You will create a function that produces the nth prime number and then returns an informational message once the prime number has been computed. In this example, we have chosen to block our to wait for completion of the computation graph by calling `get()`. This allows you to see the return value when invoking your function over HTTP. *In a production application, you should avoid blocking*, since your function will continue to run while waiting for a computation result, even though it has no useful work to do.

Create the file: `src/main/java/com/example/fn/PrimeFunction.java` with the following contents:

```java
package com.example.fn;

import java.util.function.Supplier;
import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.CloudThreads;

public class PrimeFunction {

    public String handleRequest(int nth) {

        CloudThreadRuntime rt = CloudThreads.currentRuntime();

        return rt.supply(
                () -> {
                    int num = 1, count = 0, i = 0;

                    while (count < nth) {
                        num = num + 1;
                        for (i = 2; i <= num; i++) {
                            if (num % i == 0) {
                                break;
                            }
                        }
                        if (i == num) {
                            count = count + 1;
                        }
                    }
                    return num;
                })

                .thenApply(i -> "The " + nth + "th prime number is " + i)
                .get();
    }
}
```

Edit your `func.yaml` to point to your function's entrypoint:

```
name: your_dockerhub_account/cloudthread-primes
version: 0.0.1
runtime: java
cmd: com.example.fn.PrimeFunction::handleRequest
path: /primes
```

### 3. Build and Configure your application

Create your app and deploy your function:

```
$ fn apps create cloudthreads-example
Successfully created app: cloudthreads-example

$ fn deploy cloudthreads-example
Updating route /primes using image your_dockerhub_account/cloudthread-primes::0.0.2...
```

Configure your function to talk to the local completer endpoint:

```
$ COMPLETER_SERVER_IP=`docker inspect --type container -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' completer`
$ fn apps config set cloudthreads-example COMPLETER_BASE_URL http://${COMPLETER_SERVER_IP}:8081
```

### 4. Run your Cloud Thread function

You can now run your function using `fn call` or HTTP and curl:

```
$ echo 10 | fn call cloudthreads-example /primes
The 10th prime number is 29
```

```
$ curl -XPOST -d "10" http://localhost:8080/r/cloudthreads-example/primes
The 10th prime number is 29
```

### 5. Where Do I Go from Here?

For a more realistic application that leverages the non-blocking functionality of Cloud Threads, please take a look at the asynchronous [thumbnail generation example](../examples/async-thumbnails/README.md).


# Passing data between completion stages

Cloud threads executes your code asynchronously and where possible in parallel in a distributed environment on the Fn platform - you should assume that each Cloud Threads *stage* (a lambda attached to a step of the computation) may execute on a different machine within the network.

In order to facilitate this, the Fn Java FDK will serialize each of the stage lambdas (and any captured variables) using [Java Serialization](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html) and store them remotely on the completer before executing them on the functions platform. In order to write Cloud Threads applications, you should be aware of the impact of this within your code, and how this might differ from your experience of programming within a single JVM.

The following lists the key areas that you should be aware of:

### Serializable data and captured variables
In order for the Cloud Threads completer to execute each computation stage remotely, it must transmit captured data and values attached to `CloudFuture`s over the network. Cloud Threads uses standard Java Serialization to convert objects and lambda expressions in memory into a collection of bytes for transmission over the network. Hence, all values returned by stages, passed in as arguments to stages, or captured by lambdas as variables, must be serializable.

For instance the following is valid because all passed variables and return values are serializable:

```java
CloudThreadRuntime rt = CloudThreads.currentRuntime();
 int x = 10;

 CloudFuture<String> f1 = rt.supply(()->{
       String result = String.valueOf(x * 10); // x is serialized into the remote computation
        return result; // result is serialized as a result into the captured cloud future
 });
```

However, the following example is invalid, as the variable referenced inside the lambda, and the return type wrapped by `CloudFuture` are not serializable:

```java
CloudThreadRuntime rt = CloudThreads.currentRuntime();
Optional<String> result = Optional.of("hello");

CloudFuture<NotSerializableClass> f1 = rt.supply(()->{
       String result = optional.orElse("foo"); // this will fail  as Optionals are not serializable

       return new NonSerializableClass(result);;
 });  // the execution of this stage will fail as the result is not serializable.
```

#### Capturing the function instance in a lambda
An important consideration is that, if your lambda captures fields from your function class, then that class must also be Serializable:

```java
public class MyFunction{
   private String config  = "foo";

   public void run(){
      CloudThreads.currentRuntime().supply(()->{
         // this will fail as MyFunction is not serializable
         System.err.println(config);

        });
   }
}
```

If your function fields are all immutable and serializable data (e.g. configuration parameters), then we recommend making the function class itself serializable to pass this state around.

E.g. making `MyFunction` serializable will work as the function instance object will be captured alongside the lambda:

```java
public class MyFunction implements Serializable{
   private String config  = "foo";

   public void run(){
      CloudThreads.currentRuntime().supply(()->{
         // this will work as MyFunction is captured and serialized into the lambda
         System.err.println(config);
        });
   }
}
```

Using this approach is suitable for simple cases where only immutable data is stored in your function object. However, you may find that you have non-serializable objects (such as database connections) that make this approach unusable.

In this case, you can capture any lambda arguments explicitly as variables prior to passing them, removing the need to make the function class serializable. For example:

```java
public class MyFunction{
   private final Database db; // non-serializable object
   private final String config  = "foo";

   public MyFunction(RuntimeContext ctx){
       this.config = ctx.getConfigurationByKey("ConfigKey");
       db = new Database();
   }

   public void run(){
      final String config = this.config;
      String dbVal = db.getValue();

      CloudThreads.currentRuntime().supply(()->{
         System.err.println(config );
        });
   }
}
```

Alternatively, you can make non-serializable fields `transient` and construct them on the fly:

```java
public class MyFunction implements Serialiable{
   private final transient Database db; // non-serializable object
   private final String config  = "foo";

   public MyFunction(RuntimeContext ctx){
       this.config = ctx.getConfigurationByKey("ConfigKey");
   }

   public Database getDb(){
       if(db == null){
           db = new Database();
       }
       return db;
   }

   public void run(){
      final String config = this.config;
      String dbVal = db.getValue();

      CloudThreads.currentRuntime().supply(()->{
         System.err.println(config  + getDb().getValue());
        });
   }
}
```
Using this approach allows you to use non-serializable fields within the scope of serializable lambdas.

### Capturing lambdas as variables

Java lambdas are not serializable by default, and as such cannot be used in captured variables to Cloud Thread stages, e.g.

```java
public class MyFunction{
   public void run(){
      Function<int,int> myfn =(x)->x+1;
      CloudThreads.currentRuntime()
        .supply(()->{ // this will fail as myfn is not serializable
         int result = myfn.apply(10);
         System.err.println(result);
        });
   }
}
```

To make lambdas serializable, you must cast them to Serializable at the point of construction. In this case, all captured variables (and any transitively captured clases) must be serializable:

```java
public class MyFunction{
   public void run(){
      Function<Integer,Integer > myfn = (Serializable  & Function<Integer,Integer>) (x)->x+1;
      CloudThreads.currentRuntime()
        .supply(()->{
           int result = myfn.apply(10);
           System.err.println(result);
        });
   }
}
```



### Cloud Threads stage lambda types:

The Cloud Threads API does not take standard Java `java.util.function` types as arguments (e.g. `java.util.function.Function`) - instead, it subclasses these types to include `Serializable` (e.g. [CloudThreads.SerFunction](../api/src/main/java/com/fnproject/fn/api/cloudthreads/CloudThreads.java)).

This is necessary, as by default the Java compiler does not generate the necessary code to serialize and deserialze generated lambda objects.


Generally, we recommend that you call methods on `CloudThreadRuntime` and `CloudFuture` directly (i.e. including the lambda inline with the argument) :

```java
      CloudThreads.currentRuntime()
        .supply(()->{
           int result = myfn.apply(10);
           System.err.println(result);
        });
```
and
```java
      CloudFuture<String> f1 = ...;
      f1.thenApply(String::toUpperCase);
```

In the case where you want to capture these lambdas as variables, you will need to use the `CloudThreads.Ser*` types at the point of declaration:

```java
      CloudThreads.SerFunction<String,String> fn = String::toUpperCase;

      CloudFuture<String> f1 = ...;
      f1.thenApply(fn);
```


### Data is passed between CloudThread stages by value

A side effect of data being serialized and deserialized as it is passed between stages is that instances are always passed by value when they are handled by `CloudFuture` or captured in lambdas. As a result, changes to objects within one stage will not impact other objects in other stages, unless they are explicitly passed between stages as a CloudFuture value.

For primitive types the *effectively final* constraint of the compiler prevents modification of captured variables. However, for reference types this is not the case:

```java
public class MyFunction{
   public void run(){
      java.util.concurrent.atomic.AtomicInteger myInt = new AtomicInteger(0);

      CloudThreads.currentRuntime()
        .supply(()->{
           // will print "0"
           System.err.println(myInt);
           myInt.incrementAndGet();
        }).thenRun(()->{
           // will always print "0"
           System.err.println(myInt);
        });
   }
}
```

Instead modified values should be passed between stages via `CloudFuture` methods:

```java
public class MyFunction{
   public void run(){
      java.util.concurrent.atomic.AtomicInteger myInt = new AtomicInteger(0);

      CloudThreads.currentRuntime()
        .supply(()->{
           // will print "0"
           System.err.println(myInt.get());
           myInt.incrementAndGet();
           return myInt;
        }).thenAccept((val)->{
           // will always print "1"
           System.err.println(val.get());
        });
   }
}
```


### Exceptions should be serializable
Finally, exceptions thrown by `CloudThread` lambda stages will be be propagated as error values to other stages - this means that those exceptions should be serializable. Exceptions are serializable by default, so generally you do not have to do anything.

If your code does throw exceptions that contain non-serializable fields, the exception will be converted into a [WrappedException](../api/src/main/java/com/fnproject/fn/api/cloudthreads/WrappedFunctionException.java) - this is a a `RuntimeException` that will preserve the original message and stacktrace of the source exception, but not any fields on the original exception.

E.g.:

```java
public class MyFunction{
   public static class MyException extends RuntimeException{
      public MyException(String message){
          super(message);
      }

      public static class NonSerializable{};
      NonSerializable nonsSerializableField = new NonSerializable();
   }

   public void run(){
      java.util.concurrent.atomic.AtomicInteger myInt = new AtomicInteger(0);

      CloudThreads.currentRuntime()
        .supply(()->{
           throw new MyException("bad times");
        }).exceptionally((e)->{
            // e will be an instance of com.fnproject.fn.api.cloudthreads.WrappedFunctionException here.
            System.err.println(e.getMessage()); // prints "bad times"
            e.printStackTrace(); // prints the original stack trace of the throw exception
        });
   }
}

```
