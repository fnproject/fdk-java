# Fn Flow  User Guide

By following this step-by-step guide you will learn to create, run and deploy
a simple Java app in Fn that leverages the Fn Flow asynchronous execution
APIs.


## What are Flows?

Fn Flow consists of a set of client-side APIs for you to use within your
Fn apps, as well as a long-running server component (the _flow service_) that
orchestrates computation beyond the life-cycle of your functions. Together,
these components enable non-blocking asynchronous execution flows, where your
function only runs when it has useful work to perform. If you have used the Java
8 [CompletionStage](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/CompletionStage.html)
and
[CompletableFuture](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/CompletableFuture.html)
APIs, a lot of the concepts will already be familiar to you.

[Advanced Topics](FnFlowsAdvancedTopics.md) provides more detail on how data serialization and error handling works with Fn Flow under the covers.

## Pre-requisites
Before you get started, you will need to be familiar with the [Fn Java
FDK](../README.md) and have the following things:

* [Fn CLI](https://github.com/fnproject/cli)
* [Fn Java FDK](https://github.com/fnproject/fn-java-sdk)
* [Fn flow](https://github.com/fnproject/flow)
* [Docker-ce 17.06+ installed locally](https://docs.docker.com/engine/installation/)
* A [Docker Hub](http://hub.docker.com) account

### Install the Fn CLI tool

To install the Fn CLI tool, just run the following:

```
$ curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh
```

This will download a shell script and execute it. If the script asks for
a password, that is because it invokes sudo.

### Log in to DockerHub

You will also need to be logged in to your Docker Hub account in order to
deploy functions.

```
$ docker login
```

### Start a local Fn server and Flow server

In a terminal, start the functions server:

```
$ fn start
```

Similarly, start the Flows server server and point it at the functions server API URL:

```
$ DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.Gateway}}' functions)

$ docker run --rm  \
       -p 8081:8081 \
       -d \
       -e API_URL="http://$DOCKER_LOCALHOST:8080/r" \
       -e no_proxy=$DOCKER_LOCALHOST \
       --name flow-service \
       fnproject/flow:latest
```

Optionally, start a flow UI container to visualize your Flow applications:

```
$ docker run --rm \
       -p 3002:3000 \
       -d \
       --name flowui \
       -e API_URL=http://$DOCKER_LOCALHOST:8080 \
       -e COMPLETER_BASE_URL=http://$DOCKER_LOCALHOST:8081 \
       fnproject/flow:ui
```

## Your first Fn Flow

### Create your first Flow application

Create a Maven-based Java Function using the instructions from the Fn Java FDK
[Quickstart](../README.md). For example:

```
$ mkdir example-flow-function && cd example-flow-function
$ fn init --runtime=java your_dockerhub_account/flow-primes
Runtime: java
function boilerplate generated.
func.yaml created

```

### Create a Flow within your Function

You will create a function that produces the nth prime number and then returns
an informational message once the prime number has been computed.  In this 
example, we have chosen to block our to wait for completion of the computation
flow by calling `get()`. This allows you to see the return value when invoking
your function over HTTP. *In a production application, you should avoid
blocking*, since your function will continue to run while waiting for
a computation result, even though it has no useful work to do.

Create the file: `src/main/java/com/example/fn/PrimeFunction.java` with the
following contents:

```java
package com.example.fn;

import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.Flows;

public class PrimeFunction {

    public String handleRequest(int nth) {

        Flow fl = Flows.currentFlow();

        return fl.supply(
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

Edit your `func.yaml` by changing ...
* The `cmd:` entry to your function's entrypoint
* The `path:` to `/primes`


```
name: your_dockerhub_account/flow-primes
version: 0.0.1
runtime: java
cmd: com.example.fn.PrimeFunction::handleRequest
path: /primes
```

### Build and Configure your application

Create your app and deploy your function:

```
$ fn apps create flows-example
Successfully created app: flows-example

$ fn deploy --app flows-example
Updating route /primes using image your_dockerhub_account/flow-primes::0.0.2...
```

Configure your function to talk to the local flow service endpoint:

```
$ DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.Gateway}}' functions)

$ fn apps config set flows-example COMPLETER_BASE_URL "http://$DOCKER_LOCALHOST:8081"
```

### Run your Flow function

You can now run your function using `fn call` or HTTP and curl:

```
$ echo 10 | fn call flows-example /primes
The 10th prime number is 29
```

```
$ curl -XPOST -d "10" http://localhost:8080/r/flows-example/primes
The 10th prime number is 29
```

## Asynchronous Programming Patterns

The following examples introduce the various ways in which Fn Flows enables asynchronous computation for your applications.

### Creating FlowFutures from existing values

If you already know the result of the computation:

```
  Flow fl = Flows.currentFlow();
  FlowFuture<String> stage = fl.completedValue("Hello World!);
```

or you want to create a failed stage:

```
  Flow fl = Flows.currentFlow();
  fl.failedFuture(new RuntimeException("Immediate Failure"));
```

If you want to produce a result asynchronously:

```
  Flow fl = Flows.currentFlow();
  FlowFuture<Long> stage = fl.supply(() -> {
  	 long oneHour = 60 * 60 * 1000;
  	 return System.currentTimeMillis() + oneHour;
  })
```

You can also invoke a function asynchronously and have its result complete the future once available:

```
  Flow fl = Flows.currentFlow();
  FlowFuture<HttpResponse> stage = fl.invokeFunction("myapp/myfn", HttpMethod.GET);
```

### Chaining Asynchronous Computations

By chaining FlowFutures together, you can trigger computations asynchronously once a result of a previous computation is available.

To consume the result and do some processing on it:

```
  Flow fl = Flows.currentFlow();
  FlowFuture<Long> f1 = fl.supply(() -> {
  	 long oneHour = 60 * 60 * 1000;
  	 return System.currentTimeMillis() + oneHour;
  });
  f1.thenAccept( millis -> {
  	String msg = "Time value received was " + millis;
  	System.out.println(msg);
  });
```

Similarly, you can transform the result and return the new value from the chained stage:

```
  Flow fl = Flows.currentFlow();
  FlowFuture<Long> f1 = fl.supply(() -> {
  	 long oneHour = 60 * 60 * 1000;
  	 return System.currentTimeMillis() + oneHour;
  });
  FlowFuture<Long> f2 = f1.thenApply( millis -> {
  	long seconds = millis / 1000;
  	return seconds;
  });
```

You can also chain a FlowFuture by providing a Java function that takes the previous result and itself returns a FlowFuture instance. This function is given the result of the previous computation step as its argument.

```
  Flow fl = Flows.currentFlow();
  FlowFuture<String> f1 = fl.supply(() -> "Hello");
  FlowFuture<String> f2 = f1.thenCompose( msg -> {
	return fl.supply(() -> msg + " World");
  });
```

The FlowFutures returned by _thenApply_ and _thenCompose_ are analogous to the _map_ and _flatMap_ operations provided by Java's [Stream](https://docs.oracle.com/javase/9/docs/api/java/util/stream/Stream.html) and [Optional](https://docs.oracle.com/javase/9/docs/api/java/util/Optional.html) classes. 

### Running Multiple Computations in Parallel

You can also execute two or more independent FlowFutures in parallel and combine their results once available.

To combine the results of two FlowFuture computations:

```
  Flow fl = Flows.currentFlow();
  FlowFuture<String> f1 = fl.supply(() -> "Hello");
  FlowFuture<String> f2 = fl.supply(() -> "World");
  FlowFuture<Integer> f3 = f1.thenCombine(f2, (result1, result2) -> {
  	String msg = result1 + " " + result2;
  	return msg.length();
  });

```

To wait for at least one computation to complete before invoking the next stage:

```
  Flow fl = Flows.currentFlow();
  FlowFuture<String> f1 = fl.supply(() -> {
  	try {
  		Thread.sleep((long)(Math.random() * 5000));
  	} catch(Exception e) {}
  	return "Hello";
  });
  FlowFuture<String> f2 = fl.supply(() -> {
  	try {
  		Thread.sleep((long)(Math.random() * 5000));
  	} catch(Exception e) {}
  	return "World";
  });
  fl.anyOf(f1, f2).thenApply(result -> ((String)result).toUpperCase());
```

You can also wait for all computations to complete before continuing. Simply replace the last line above with:

```
  fl.allOf(f1, f2).thenApply(ignored -> f1.get() + " " + f2.get());
```

Since the _allOf_ stage above returns a void value, you must explicitly retrieve the results of the stages you are interested in within the lambda expression.

### Handling Errors

There are several methods for handling errors with FlowFutures: 

`exceptionally` allows you to recover from the exceptional completion of a FlowFuture by transforming exceptions to the original type of the future:

```
	Flow fl = Flows.currentFlow();
	FlowFuture<Integer> f1 = fl.supply(() -> {
		if (System.currentTimeMillis() % 2L == 0L) {
			throw new RuntimeException("Error in stage");
		}
		return 100;
    }).exceptionally(err -> -1);
```

`exceptionallyCompose` is similar but allows you to handle an exception by executing one or more other nodes and attaching the subsequent FlowFuture to the result.
```
	Flow fl = Flows.currentFlow();
	FlowFuture<Integer> f1 = fl.supply(() -> {
		if (System.currentTimeMillis() % 2L == 0L) {
			throw new RuntimeException("Error in stage");
		}
		return 100;
    }).exceptionallyCompose(err -> fl.invokeFunction("./recover",new RecoveryAction());
```


`handle` is similar  to `exceptionally` but lets you deal with either the exception or the value in a single stage with a Java function that takes two parameters. In the case of success the exception value will be null and in the case of an exception the value will be null.

```
	Flow fl = Flows.currentFlow();
	FlowFuture<String> f1 = fl.supply(() -> {
		if (System.currentTimeMillis() % 2L == 0L) {
			throw new RuntimeException("Error in stage");
		}
		return 100;
	}).handle((val, err) -> {
		if (err != null){
			return "An error occurred in this function";
		} else {
			return "The result was good: " + val;
		}
	});
```



### Where Do I Go from Here?

For a more realistic application that leverages the non-blocking functionality
of Fn Flow, please take a look at the asynchronous [thumbnail generation
example](../examples/async-thumbnails/README.md).

[Advanced Topics](FnFlowsAdvancedTopics.md) provides a more in-depth treatment of data serialization and error handling with Fn Flow.
