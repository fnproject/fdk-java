# Example oFunctions Project: String Reverse

This example provides an HTTP endpoint for reversing strings

```bash
$ curl -d "Hello, World!" "http://localhost:8080/r/string-reverse-app/reverse"
!dlroW ,olleH
```


## Demonstrated FDK features

This example uses **no** features of the fn Java FDK; in fact it doesn't have
a dependency on the fn Java FDK, it just plain old Java code.

## Step by step

Ensure you have the functions server running using, this will host your
function and provide the HTTP endpoints that invoke it:

```bash
$ fn start
```

Build the function locally

```bash
$ fn build
```

Create an app and route to host the function

```bash
$ fn apps create string-reverse-app
$ fn routes create string-reverse-app /reverse
```

Invoke the function to reverse a string

```bash
$ curl -d "Hello, World!" "http://localhost:8080/r/string-reverse-app/reverse"
!dlroW ,olleH
```


## Code walkthrough

The entrypoint to the function is specified in `func.yaml` in the `cmd` key.
It is set this to `com.fnproject.fn.examples.StringReverse::reverse`. The whole class
`StringReverse` is shown below:


```java
package com.fnproject.fn.examples;

public class StringReverse {
    public String reverse(String str) {
        StringBuilder builder = new StringBuilder();
        for (int i = str.length() - 1; i >= 0; i--) {
            builder.append(str.charAt(i));
        }
        return builder.toString();
    }
}
```

As you can see, this is plain java with no references to the fn API. The
fn Java FDK handles the marshalling of the HTTP body into the `str`
parameter as well as the marshalling of the returned reversed string into the HTTP
response body (see [Data Binding](/docs/DataBinding.md) for more
information on how marshalling is performed).


