# Fn Flow - Advanced Topics


In spite of many similarities with Java's [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) and [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html), Flow runs on the Fn platform and is inherently distributed. As a result, special distributed system considerations apply, including data serialization, failure and retry semantics of remote systems, and application-level error handling.

## Passing data between completion stages

Fn Flow executes your code asynchronously and where possible in parallel
in a distributed environment on the Fn platform - you should assume that each
Flow *stage* (a lambda attached to a step of the computation) may
execute on a different machine within the network.

In order to facilitate this, the Fn Java FDK will serialize each of the stage
lambdas (and any captured variables) using [Java
Serialization](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)
and store them remotely in the flow service before executing them on the functions
platform. In order to write Flows applications, you should be aware of
the impact of this within your code, and how this might differ from your
experience of programming within a single JVM.

The following lists the key areas that you should be aware of:

### Serializable data and captured variables
In order for the flow service to execute each computation stage
remotely, it must transmit captured data and values attached to `FlowFuture`s
over the network. Fn Flow uses standard Java Serialization to convert
objects and lambda expressions in memory into a collection of bytes for
transmission over the network. Hence, all values returned by stages, passed in
as arguments to stages, or captured by lambdas as variables, must be
serializable.

For instance the following is valid because all passed variables and return
values are serializable:

```java
Flow fl = Flows.currentFlow();
 int x = 10;

 FlowFuture<String> f1 = fl.supply(()->{
       String result = String.valueOf(x * 10); // x is serialized into the remote computation
        return result; // result is serialized as a result into the captured flow future
 });
```

However, the following example is invalid, as the variable referenced inside
the lambda, and the return type wrapped by `FlowFuture` are not serializable:

```java
Flow fl = Flows.currentFlow();
Optional<String> result = Optional.of("hello");

FlowFuture<NotSerializableClass> f1 = fl.supply(()->{
       String result = optional.orElse("foo"); // this will fail  as Optionals are not serializable

       return new NonSerializableClass(result);;
 });  // the execution of this stage will fail as the result is not serializable.
```

### Capturing the function instance in a lambda
An important consideration is that, if your lambda captures fields from your
function class, then that class must also be Serializable:

```java
@FnFeature(FlowFeature.class)
public class MyFunction{
   private String config  = "foo";

   public void run(){
      Flows.currentFlow().supply(()->{
         // this will fail as MyFunction is not serializable
         System.err.println(config);

        });
   }
}
```

If your function fields are all immutable and serializable data (e.g.
configuration parameters), then we recommend making the function class itself
serializable to pass this state around.

E.g. making `MyFunction` serializable will work as the function instance object
will be captured alongside the lambda:

```java
@FnFeature(FlowFeature.class)
public class MyFunction implements Serializable{
   private String config  = "foo";

   public void run(){
      Flows.currentFlow().supply(()->{
         // this will work as MyFunction is captured and serialized into the lambda
         System.err.println(config);
        });
   }
}
```

Using this approach is suitable for simple cases where only immutable data is
stored in your function object. However, you may find that you have
non-serializable objects (such as database connections) that make this approach
unusable.

In this case, you can capture any lambda arguments explicitly as variables
prior to passing them, removing the need to make the function class
serializable. For example:

```java
@FnFeature(FlowFeature.class)
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

      Flows.currentFlow().supply(()->{
         System.err.println(config );
        });
   }
}
```

Alternatively, you can make non-serializable fields `transient` and construct
them on the fly:

```java
@FnFeature(FlowFeature.class)
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
      String dbVal = getDb().getValue();

      Flows.currentFlow().supply(()->{
         System.err.println(config  + getDb().getValue());
        });
   }
}
```
Using this approach allows you to use non-serializable fields within the scope
of serializable lambdas.

### Capturing lambdas as variables

Java lambdas are not serializable by default, and as such cannot be used in
captured variables to Flow stages, e.g.

```java
public class MyFunction{
   public void run(){
      Function<int,int> myfn =(x)->x+1;
      Flows.currentFlow()
        .supply(()->{ // this will fail as myfn is not serializable
         int result = myfn.apply(10);
         System.err.println(result);
        });
   }
}
```

To make lambdas serializable, you must cast them to Serializable at the point
of construction. In this case, all captured variables (and any transitively
captured clases) must be serializable:

```java
public class MyFunction{
   public void run(){
      Function<Integer,Integer > myfn = (Serializable & Function<Integer,Integer>) (x)->x+1;
      Flows.currentFlow()
        .supply(()->{
           int result = myfn.apply(10);
           System.err.println(result);
        });
   }
}
```



### Flow stage lambda types:

The Fn Flow API does not take standard Java `java.util.function` types as
arguments (e.g. `java.util.function.Function`) - instead, it subclasses these
types to include `Serializable` (e.g.
[Flows.SerFunction](../api/src/main/java/com/fnproject/fn/api/flow/Flows.java)).

This is necessary, as by default the Java compiler does not generate the
necessary code to serialize and deserialze generated lambda objects.


Generally, we recommend that you call methods on `Flow` and
`FlowFuture` directly (i.e. including the lambda inline with the argument) :

```java
      Flows.currentFlow()
        .supply(()->{
           int result = myfn.apply(10);
           System.err.println(result);
        });
```
and
```java
      FlowFuture<String> f1 = ...;
      f1.thenApply(String::toUpperCase);
```

In the case where you want to capture these lambdas as variables, you will need
to use the `Flows.Ser*` types at the point of declaration:

```java
      Flows.SerFunction<String,String> fn = String::toUpperCase;

      FlowFuture<String> f1 = ...;
      f1.thenApply(fn);
```


### Data is passed between Flow stages by value

A side effect of data being serialized and deserialized as it is passed between
stages is that instances are always passed by value when they are handled by
`FlowFuture` or captured in lambdas. As a result, changes to objects within
one stage will not impact other objects in other stages, unless they are
explicitly passed between stages as a FlowFuture value.

For primitive types the *effectively final* constraint of the compiler prevents
modification of captured variables. However, for reference types this is not
the case:

```java
public class MyFunction{
   public void run(){
      java.util.concurrent.atomic.AtomicInteger myInt = new AtomicInteger(0);

      Flows.currentFlow()
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

Instead modified values should be passed between stages via `FlowFuture`
methods:

```java
public class MyFunction{
   public void run(){
      java.util.concurrent.atomic.AtomicInteger myInt = new AtomicInteger(0);

      Flows.currentFlow()
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
Finally, exceptions thrown by `Flow` lambda stages will be be propagated
as error values to other stages - this means that those exceptions should be
serializable. Exceptions are serializable by default, so generally you do not
have to do anything.

If your code does throw exceptions that contain non-serializable fields, the
exception will be converted into
a [WrappedException](../api/src/main/java/com/fnproject/fn/api/flow/WrappedFunctionException.java)
- this is a a `RuntimeException` that will preserve the original message and
stacktrace of the source exception, but not any fields on the original
exception.

E.g.:

```java
@FnFeature(FlowFeature.class)
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

      Flows.currentFlow()
        .supply(()->{
           throw new MyException("bad times");
        }).exceptionally((e)->{
            // e will be an instance of com.fnproject.fn.api.flow.WrappedFunctionException here.
            System.err.println(e.getMessage()); // prints "bad times"
            e.printStackTrace(); // prints the original stack trace of the throw exception
        });
   }
}

```
