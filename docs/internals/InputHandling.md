# How is function input handled and dispatched

## Input Message decoding 
The function receives an input message via one of two routes, depending on how the function is configured: 

1. For a one-shot function the body of the event is passed on stdin and headers are passed in one or more environment variables, output is printed directly to stdout. 

2. For a hot function the input messages are streamed to stdin via an HTTP frame and each invocation should stream an HTTP response to its std out (with content-length delimiters)

The Java FDK interprets either of these via [StdIOEntryPoint](../../src/main/java/com.fnproject.fn/runtime/StdIOEntryPoint.java) and builds an [InputMessage](../../src/main/java/com.fnproject.fn/runtime/InputMessage.java)  using one of the available [EventCodec](../..//src/main/java/com.fnproject.fn/runtime/EventCodec.java)

Examples are [HttpEventCodec](../..//src/main/java/com.fnproject.fn/runtime/HttpEventCodec.java) and [DefaultEventCodec](../..//src/main/java/com.fnproject.fn/runtime/DefaultEventCodec.java)

InputEvents do not read the full  input stream by default - this can be consumed exactly once . 

##Invoke Dispatch 

Once an event is unmarshalled from the input  it is proffered to the configured FunctionInvoker classes - these can grab the event based on metadata (e.g. headers, path) and must hand the event its entirety. 

The default FunctionInvoker is the MethodFunctionInvoker  - this invokes a method on a configured class using the configured type coercion (see below) .

## Type Coercion 

As with Dispatch there are some registered Input and Output type coercions - these are tried in priority order for each parameter. 

Input Coercions are offered the chance to read the input buffer of the function - the first coercion that does this consumes the data and no other coercions may use the input. 

There are type coercions for the body of the input event: 

* String - presents the request as a string
* InputEvent - takes the originating input event 
* Jackson binding - unmarshalls the object as a Json blob


Functions may have multiple parameters however only one may consume the input buffer. 

Output coercions work in the same way but map return values to output objects. Generally speaking returning null will cause errors. 


If you want to program directly against fn Java FDK events you can define a function like :

```java
package testfn;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.InputEvent;

 public class TestFn {
    
    OutputEvent handle(InputEvent in){
        
        in.consumeBody((inputStream)->{
            
        });
        ...
    }
 }
  
```
