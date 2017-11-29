# Example Spring Cloud Function

This is an example [spring cloud function](https://github.com/spring-cloud/spring-cloud-function) 
project running on fn using the 
[`SpringCloudFunctionInvoker`](/runtime/src/main/java/com/fnproject/fn/runtime/spring/SpringCloudFunctionInvoker.java).


```bash
$ fn deploy --local --app spring-fn
$ fn routes config set spring-fn /fn FN_SPRING_FUNCTION upperCase
$ echo "Hello World" | fn call spring-fn /fn
HELLO WORLD
$ fn routes create spring-fn /another-fn
$ fn routes config set spring-fn /another-fn FN_SPRING_SUPPLIER supplier
$ fn call spring-fn /another-fn
hello
```

## Code walkthrough

```java
@Configuration
```
Defines that the class is a 
[Spring configuration class](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Configuration.html) 
with `@Bean` definitions inside of it.

```java
@Import(ContextFunctionCatalogAutoConfiguration.class)
```
Specifies that this configuration uses a [`InMemoryFunctionCatalog`](https://github.com/spring-cloud/spring-cloud-function/blob/a973b678f1d4d6f703a530e2d9e071b6d650567f/spring-cloud-function-context/src/main/java/org/springframework/cloud/function/context/InMemoryFunctionCatalog.java)
that provides the beans necessary
for the `SpringCloudFunctionInvoker`.

```java
    ...
    @FnConfiguration
    public static void configure(RuntimeContext ctx) {
        ctx.setInvoker(new SpringCloudFunctionInvoker(FunctionConfig.class));
    }
```

Sets up the Fn Java FDK to use the SpringCloudFunctionInvoker which performs function discovery and invocation.

```java
    // Empty entrypoint that isn't used but necessary for the EntryPoint. Our invoker ignores this and loads our own
    // function to invoke
    public void handleRequest() { }
```

Currently the runtime expects a method to invoke, however this isn't used in the SpringCloudFunctionInvoker so 
we declare an empty method simply to keep the runtime happy.

```java
    @Bean
    public Supplier<String> supplier() {
        return () -> "hello";
    }

    @Bean
    public Consumer<String> consumer() {
        return System.out::println;
    }

    @Bean
    public Function<String, String> function() {
        return String::toUpperCase;
    }

    @Bean
    public Function<String, String> lowerCase() {
        return String::toLowerCase;
    }
```

Finally the heart of the configuration; the bean definitions of the functions to invoke.

The `SpringCloudFunctionInvoker` resolves functions in the following order:

Functions are resolved first based on configuration, and then type.

The bean providing the function that is invoked is resolved is as follows:

* Environment variable `FN_SPRING_FUNCTION` returning a `Function`
* Environment variable `FN_SPRING_CONSUMER` returning a `Consumer`
* Environment variable `FN_SPRING_SUPPLIER` returning a `Supplier`
* Bean named `function` returning a `Function`
* Bean named `consumer` returning a `Consumer`
* Bean named `supplier` returning a `Supplier`
