# Java CORS extention example 

This example shows how to create a java function that can be invoked from a browser by implementing CORS headers. 

It comes in two parts: 


`com.example.fn.CorsInvoker` is a function invoker which intercepts and handles 'OPTIONS' requests, this is inserted into the function chain in `com.example.fn.CorsFunction.init`

In func.yaml we add the actual CORS  headers to the function :

```
headers:
  Access-Control-Allow-Headers:
  - Authorization, Origin, X-Requested-With, Content-Type, Accept
  Access-Control-Allow-Methods:
  - POST, GET, OPTIONS
  Access-Control-Allow-Origin:
  - '*'
```

