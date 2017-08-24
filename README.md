# fn Java Functions Developer Kit (FDK)

This project adds support for writing functions in Java on the fn platform.


# Quick Start Tutorial

By following this step-by-step guide you will learn to create, run and deploy a simple app written in Java on fn.

## Pre-requisites

Before you get started you will need the following things:

* [The fn CLI](https://github.com/fnproject/fn#install-cli-tool)
* [Docker-ce 17.06+ installed locally](https://docs.docker.com/engine/installation/)

## Your first Function

### 1. Create your first Java Function:

```bash
$ mkdir hello-java-function && cd hello-java-function
$ fn init --runtime=java jbloggs/hello
Runtime: java
function boilerplate generated.
func.yaml created
```

This creates a new Maven based Java Function which includes some
boilerplate to get you started. The `pom.xml` includes a dependency on
the latest version of the fn Java FDK that is useful for developing your
Java functions.

Note that the `jbloggs/hello` name follows the format of a Docker image
name. These tutorials are running locally, so you will not need to push
images to a Docker registry, but in a real scenario you would deploy to
a remote registry and therefore you should replace `jbloggs` with your
user name.

You can now import this project into your favourite IDE as normal.

### 2. Deep dive into your first Java Function:

We'll now take a look at what makes up our new Java Function. First,
lets take a look at the `func.yaml`:

```bash
$ cat func.yaml
name: jbloggs/hello
version: 0.0.1
runtime: java
cmd: com.example.faas.HelloFunction::handleRequest
path: /hello
```

The `cmd` field determines which method is called when your funciton is invoked. In the generated Function, the `func.yaml` references `com.example.faas.HelloFunction::handleRequest`.

Open the file: `src/main/java/com/example/faas/HelloFunction.java`:

```java
package com.fnproject.fn.examples;

public class HelloFunction {

    public String handleRequest(String input) {
        String name = (input == null || input.isEmpty()) ? "world"  : input;
        return "Hello, " + name + "!";
    }

}
```

### 3. Run your first Java Function:
You are now ready to run your Function using the `fn` CLI tool.

This may take a minute the first time as you will need pull in some new dependencies in order to build your Function.

*FIXME: Oracle Internal*:

1. Ensure you have the `http_proxy` and `https_proxy` set and also add `10.167.103.241` to your `no_proxy`.  Make sure your docker settings also include the proper proxies.
2. Log in to `registry.oracledx.com`: `docker login -u agent -p gf21\&54G registry.oracledx.com`

```bash
$ fn run
Building image jbloggs/hello:0.0.1
Sending build context to Docker daemon  9.728kB
Step 1/12 : FROM maven:3.5-jdk-8-alpine as build-stage
 ---> 3c2b824cf55f
Step 2/12 : WORKDIR /function
 ---> d3bf7a06081b
 ...
Step 5/12 : RUN mvn package dependency:go-offline -DstripVersion=true -Dmdep.prependGroupId=true dependency:copy-dependencies
 ---> Running in 58b3b1397ba2
[INFO] Scanning for projects...
Downloading: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-compiler-plugin/3.3/maven-compiler-plugin-3.3.pom
Downloaded: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-compiler-plugin/3.3/maven-compiler-plugin-3.3.pom (11 kB at 21 kB/s)
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 2.228 s
[INFO] Finished at: 2017-06-27T12:06:59Z
[INFO] Final Memory: 18M/143M
[INFO] ------------------------------------------------------------------------
...
Function jbloggs/hello:0.0.1 built successfully.
Hello, world!
```

The next time you run this, it will execute much quicker as your dependencies are cached. Try passing in some input this time:

```bash
$ echo -n "Michael Faasbender" | fn run
...
Hello, Michael Faasbender!
```

### 4. Making changes to your Function

Making code changes and trying them out is simple. For example, lets
localize our function.

Update the function class to match:

```java
package com.example.faas;

import java.util.Locale;
import java.util.ResourceBundle;

public class HelloFunction {

    public String handleRequest(String input) {
        String name = (input == null || input.isEmpty()) ? "world"  : input;
        Locale.setDefault(new Locale((null == System.getenv("LANG")) ?
                          Locale.getDefault().toString() :
                                     System.getenv("LANG")));
        ResourceBundle bundle =
            ResourceBundle.getBundle("com.example.faas.HelloFunction");

        return bundle.getString("greeting") + ", " + name + "!";
    }

}
```

And you need to create the properties files for some locales.  We'll do
`HelloFunction.properties` and `HelloFunction_de.properties`,
respectively.

```bash
greeting=Hello
```

```bash
greeting=Guten Tag
```

Now we can run this, passing the LANG env var, like so

```bash
echo -n "Michael Faasbender" | fn run --env LANG=en
```

We will see the same output as before,

```bash
Function jbloggs/hello:0.0.1 built successfully.
Hello, Michael Faasbender!
```

But we can localize it by changing the `LANG` env var.

```bash
echo -n "Michael Faasbender" | fn run --env LANG=de
```

```bash
Function jbloggs/hello:0.0.1 built successfully.
Guten Tag, Michael Faasbender!
```

This brings up an interesting point.  The auto-generated test


Before running this, we also need to update the corresponding function test class:

```bash
$ mv src/test/java/com.fnproject.fn.examples/HelloFunctionTest.java src/test/java/com.fnproject.fn.examples/HolaFunctionTest.java
```
```java
package com.fnproject.fn.examples;

import com.fnproject.fn.testing.FnTestingRule;
import org.junit.*;

import static org.junit.Assert.*;
import static com.fnproject.fn.testing.FnTestingRule.*;

public class HolaFunctionTest {

    @Rule
    public final FnTestingRule testing = FnTestingRule.createDefault();

    @Test
    public void shouldReturnGreeting() {
        testing.givenEvent().enqueue();
        testing.thenRun(HolaFunction.class, "handleRequest");

        FnResult result = testing.getOnlyResult();
        assertEquals("Hola, mundo!", result.getBodyAsString());
    }

}
```

Testing functions is covered in more detail in [Testing Functions](docs/TestingFunctions.md).


You can now run your updated function:

```bash
$ fn run
...
Hola, mundo!
```

### 6. Test using HTTP and the local `functions` server
The previous example used `fn run` to run a function directly via docker, you can also  use the `functions` server locally to test HTTP calls to your functions.

Start the `functions` server by running its docker image locally:

```bash
$ docker run -p8080:8080 -d -v /var/run/docker.sock:/var/run/docker.sock fnproject/functions:latest
```

*FIXME:* in the future you'll be able to just run `fn start` but at the moment the `fn` tool is not using the latest docker image for the server

Create an `app`:

```bash
$ fn apps create java-app
Successfully created app: java-app
```

Bind your Function to a `route`:

```bash
$ fn routes create java-app /hola
/hola created with jbloggs/hello:0.0.1
```

Call the Function via the `fn` CLI:

```bash
$ fn call java-app /hola
Hola, mundo!
```

You can also call the Function via curl:

```bash
$ curl http://localhost:8080/r/java-app/hola
Hola, mundo!
```

### 7. Something more interesting
The fn Java FDK supports [flexible data binding](docs/DataBinding.md)  to make it easier for you to map function input and output data to Fava objects.

Below is an example to of a Function that returns a Pojo which will be serialized to JSON using Jackson:

```java
package com.fnproject.fn.examples;

public class PojoFunction {

    public static class Greeting {
        public final String name;
        public final String salutation;

        public Greeting(String salutation, String name) {
            this.salutation = salutation;
            this.name = name;
        }
    }

    public Greeting greet(String name) {
        if (name == null || name.isEmpty())
            name = "World";

        return new Greeting("Hello", name);
    }

}
```


Update your `func.yaml` to reference the new method:

```yaml
cmd: com.fnproject.fn.examples.PojoFunction::greet
```

Now run your new function:

```bash
$ fn run
...
{"name":"World","salutation":"Hello"}

$ echo -n Michael | fn run
...
{"name":"Michael","salutation":"Hello"}
```

## 8. Where do I go from here?

Learn more about the fn Java FDK by reading the next tutorials in the series. Checkout the examples in the [`examples` directory](examples) for some functions demonstrating different features of the fn Java FDK.

### Configuring your function

If you want to set up the state of your function object before the function is invoked, and to use external configuration variables that you can set up with the `fn` tool, have a look at the [Function Configuration](docs/FunctionConfiguration.md) tutorial.

### Input and output bindings

You have the option of taking more control of how serialization and deserialization is performed by defining your own bindings.

See the [Data Binding](docs/DataBinding.md) tutorial for other out-of-the-box options and the [Extending Data Binding](docs/ExtendingDataBinding.md) tutorial for how to define and use your own bindings.

### Asynchronous workflows

Suppose you want to call out to some other function from yours - perhaps a function written in a different language, or even one maintained by a different team. Maybe you then want to do some processing on the result. Or even have your function interact asynchronously with a completely different system. Perhaps you also need to maintain some state for the duration of your function, but you don't want to pay for execution time while you're waiting for someone else to do their work.

If this sounds like you, then have a look at the [Cloud Threads quickstart](docs/CloudThreadsUserGuide.md).



# Contributing

*TBD*
