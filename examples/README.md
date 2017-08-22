# fn Example Projects

In this directory you will find 3 example projects demonstrating different
features of the fn Java FDK:

* Plain java code support (`string-reverse`)
* Functional testing of your functions (`regex-query` and `qr-code`)
* Built-in JSON coercion (`regex-query`)
* [InputEvent and OutputEvent](/docs/DataBinding.md) handling (`qr-code`)

## 1. String reverse

This function takes a string and returns the reverse of the string.
The fn Java FDK runtime will handle marshalling data into your
functions without the function having to have any knowledge of the SDK.

## 2. Regex query

This function takes a JSON object containing a `text` field and a `regex`
field and return a JSON object with a list of matches in the `matches`
field. It demonstrates the builtin JSON support of the fn Java
wrapper (provided through Jackson) and how the platform handles serialisation
of POJO return values.

## 3. QR Code gen

This function parses the query parameters of a GET request (through the
`InputEvent` passed into the function) to generate a QR code. It demonstrates
the `InputEvent` and `OutputEvent` interfaces which provide low level
access to data entering the fn Java FDK.
