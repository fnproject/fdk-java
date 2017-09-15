# `fn` Java FDK Example Projects

In this directory you will find some example projects demonstrating different
features of the `fn` Java FDK:

* Plain java code support (`string-reverse`)
* Functional testing of your functions (`regex-query` and `qr-code`)
* Built-in JSON coercion (`regex-query`)
* [InputEvent and OutputEvent](/docs/DataBinding.md) handling (`qr-code`)

## 1. String reverse

This function takes a string and returns the reverse of the string.
The `fn` Java FDK runtime will handle marshalling data into your
functions without the function having to have any knowledge of the FDK API.

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
access to data entering the `fn` Java FDK.

## 4. Asynchronous thumbnails generation

This example showcases the Fn Flow asynchronous execution API, by
creating a workflow that takes an image and asynchronously generates three
thumbnails for it, then uploads them to an object storage.
