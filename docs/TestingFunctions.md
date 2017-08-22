# Testing your functions

The functions testing harness allows you to quickly build and test your Java functions in your IDE and test them in an emulated function runtime without uploading your functions to the cloud. The framework uses [JUnit 4](http://junit.org/junit4/) rules.

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

Suppose you have [written a simple function](QuickStart.md) as follows:

```java
package com.fnproject.fn.examples;

public class MyFn {
  public String handleRequest(String input){
    return new StringBuilder(input).reverse().toString();
  }
}
```

The testing harness is a [JUnit Rule](https://github.com/junit-team/junit4/wiki/rules) that you should create as part of your test classes.

Create a test class that instantiates the testing rule as follows:

```java
package com.fnproject.fn.examples;

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

