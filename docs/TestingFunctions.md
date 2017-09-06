# Testing your functions

The Fn Java FDK testing harness allows you to quickly build and test your Java functions in your IDE and test them in an emulated function runtime without uploading your functions to the cloud. The framework uses [JUnit 4](http://junit.org/junit4/) rules.

## Add the test module to your project

To import the testing library add the following dependency to your Maven project with `<scope>test</scope>`.

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.12</version> <!-- Most recent version of JUnit should work-->
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.fnproject.fn</groupId>
    <artifactId>testing</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Writing your first test

Suppose you have [written a simple function](../README.md) as follows:

```java
package com.example.fn;

public class MyFn {
  public String handleRequest(String input){
    return new StringBuilder(input).reverse().toString();
  }
}
```

The testing harness is a [JUnit Rule](https://github.com/junit-team/junit4/wiki/rules) that you should create as part of your test classes.

Create a test class that instantiates the testing rule as follows:

```java
package com.example.fn;

import org.junit.Rule;
import org.junit.Test;

import com.fnproject.fn.testing.FnTestingRule;

public class FunctionTest {
  @Rule
  public final FnTestingRule testing = FnTestingRule.createDefault();

  @Test
  public void shouldReverseStrings() {
    // ...
  }
}
```

The testing rule is stateful and should be created once per test (as above).

The following sends an event with a simple string body to your function, and then checks the response which can be retrieved from the test harness as an `FnResult` object.

```java
  @Test
  public void shouldReverseStrings() {
    testing.givenEvent().withBody("Hello").enqueue();

    testing.thenRun(MyFn.class, "handleRequest");

    Assert.assertEquals("olleH", testing.getOnlyResult().getBodyAsString());
  }
```

It is also possible to send multiple events and then check the pending responses. This simulates the behaviour of *hot functions*. With hot functions all enqueued events will be executed by the runtime before results can be checked; use `getResults()` to get a list of `FnResult` objects that describe the output.

```java
  @Test
  public void shouldReverseStrings() {
    testing.givenEvent().withBody("One").enqueue();
    testing.givenEvent().withBody("Two").enqueue();
    testing.givenEvent().withBody("Three").enqueue();

    testing.thenRun(MyFn.class, "handleRequest");

    List<FnResult> results = testing.getResults();
    Assert.assertEquals(results.size(), 3);
    FnResult theResult = results.get(0);
    Assert.assertEquals("enO", theResult.getBodyAsString());
    theResult = results.get(1);
    Assert.assertEquals("owT", theResult.getBodyAsString());
    theResult = results.get(2);
    Assert.assertEquals("eerhT", theResult.getBodyAsString());
  }
```

For enqueuing the same event multiple times you can also pass the number of calls to enqueue as follows:

```java
    testing.givenEvent().withBody("someEvent").enqueue(10);
```

# Testing data binding and configuration
The testing harness replicates the behavior of the functions platform, including [data binding](DataBinding.md) and [configuration](FunctionConfiguration.md).

Given a class that uses POJO data binding and some initialization:

```java
public class MyFnBinding{
  public static class DataInput{
    public String a;
    public String b;
  }

  public static class DataOutput{
    public String c;
  }

  private String prefix;

  @FnConfiguration
  public void configure(RuntimeContext ctx) {
    prefix = ctx.getConfigurationByKey("PREFIX").orElse("");
  }

  public DataOutput handleRequest(DataInput input){
    DataOutput output  = new DataOutput();
    output.c = prefix + input.a + input.b;
    return output;
  }
}
```

You can test that this is all handled correctly as follows:

```java
  @Test
  public void shouldHandleInput() {
    testing.setConfig("PREFIX", "blah-");

    testing
      .givenEvent()
      .withHeader("content-type", "application/json")
      .withBody("{\"a\": \"foo\", \"b\":\"bar\"}")
      .enqueue();

    testing.thenRun(MyFnBinding.class, "handleRequest");

    FnResult result = testing.getOnlyResult();
    Assert.assertEquals("application/json", result.getHeaders().get("content-type").get());
    Assert.assertEquals("{\"c\":\"blah-foobar\"}", result.getBodyAsString());
  }
```

# Testing Cloud Threads

Even if your function is using the [Cloud Threads API](CloudThreadsUserGuide.md), you will need no additional plumbing
to emulate the functionality of the Cloud Threads completions service. The `FnTestingRule` already provides a simulated
completions service that will deal with any Cloud Threads invocation. The `thenRun` method will not only execute the
function under test but also wait for all the asynchronous completions to finish.

Functions only return the data from the initial invocation, so you will not have direct access to the return values of
each asynchronous process spawned as a Cloud Thread. Therefore, you will be unable to test your function through the I/O
contract alone and you will want to test the functions side effects (using mocks/spies).
This can involve for example testing the changes to a mock database, or capturing HTTP requests to a mock HTTP server;
you can use your favourite test framework for handling such mocks.

One thing that is however specific to the Fn platform is the invocation of other functions, i.e. when
you use Cloud Threads to asynchronously call a function with the `invokeFunction()` API. This can be
mocked with the `FnTestingRule` rule itself, which provides a convenient API for pretending that the execution of a
remote function results in a valid return value or an error.

You can specify that the invocation a function returns a valid value (as a byte array):

```java
  @Test
  public void callsRemoteFunctionWhichSucceeds() {

    testing.givenFn("example/other-function").withResult("blah".getBytes());

    // ...

  }
```

Or you can specify that the invocation a function will cause a user error or a platform error:

```java
  @Test
  public void callsRemoteFunctionWhichCausesAnError() {

    testing.givenFn("example/other-function").withFunctionError();
    testing.givenFn("example/other-function-2").withPlatformError();

    // ...

  }
```

You can even specify custom actions to perform when the function is called, using for example a lambda. This can be
used to check some behavior:

```java

  static boolean called;

  @Test
  public void callsRemoteFunction() {

    testing.givenFn("example/other-function").withAction( (data) -> { called = true; return data; } );

    called = false;

    // ... prepare an event and run the function ...

    Assert.assertTrue(called);
  }
```

This is a very simple example using a static variable - but any mock injection can be performed provided that the mock
object is effectively final and can be captured in the lambda.

Also note that `withAction` makes no thread-safety guarantees, and Cloud Threads are executed asynchronously and
concurrently. Therefore, if you have several `withAction` clauses accessing the same shared state, you will have to take
care of synchronization of the state.

# Sharing data between tests and your functions 
To ensure isolation of each function invocation and/or Cloud Threads completion, and to simulate the behaviour of the
real Fn platform (where each function invocation can potentially run in a different JVM), the `FnTestingRule` runs each `thenRun` invocation and each Cloud Thread completion using a different Java Class Loader. 

While this improves the veracity of tests, it prevents your tests from accessing or modifying the state of your functions and vice versa. 

If you need to share objects or static data between your test classes and your functions (i.e. to pre-initialize global state) you can do so within your tests using the `addSharedClass` (for a specific class) and `addSharedPrefix` (for a package, or class prefix) methods on `FnTestingRule`. 

```java
    testing.addSharedClass(MyClassWithStaticState.class); // Shares only the specific class
    testing.addSharedPrefix("com.example.MyClassWithStaticState"); // Shares the class and anything under it
    testing.addSharedPrefix("com.example.mysubpackage."); // Shares anyhting under a package
```

While it is possible, it is not generally correct to share the function class itself with the test Class Loader - doing so may result in unexpected (not representative of the real fn platform) initialisation of static fields on the class. With Cloud Threads sharing the test class may also result in concurrent access to static data (via `@FnConfiguration` methods). 