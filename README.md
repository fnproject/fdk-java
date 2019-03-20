[![CircleCI](https://circleci.com/gh/fnproject/fdk-java.svg?style=svg&circle-token=348bec5610c34421f6c436ab8f6a18e153cb1c01)](https://circleci.com/gh/fnproject/fdk-java)

# Fn Java Functions Developer Kit (FDK)
The Java Function Developer Kit (FDK) makes it easy to deploy Java functions to Fn with full support for Java 11+ as the default out of the box.

## Install
MacOS installation:
```sh
brew update && brew install fn
```

or

Alternatively for Linux/Unix/MacOS:

```sh
curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh
```

## General Information
* See the Fn [Quickstart](https://github.com/fnproject/fn/blob/master/README.md) for sample commands.
* [Detailed installation instructions](http://fnproject.io/tutorials/install/).
* [Configure your CLI Context](http://fnproject.io/tutorials/install/#ConfigureyourContext).
* For a list of commands see [Fn CLI Command Guide and Reference](https://github.com/fnproject/docs/blob/master/cli/README.md).
* For general information see Fn [docs](https://github.com/fnproject/docs) and [tutorials](https://fnproject.io/tutorials/).

## Fn Java FDK Development Information
### Contributing
Please see [CONTRIBUTING.md](CONTRIBUTING.md).

### Fn Java FDK FAQ
Some common questions are answered in [our FAQ](docs/FAQ.md).

### Fn Java FDK Tutorials and Examples

#### Examples
Check out Java FDK examples in the [`examples`](examples) directory for
functions demonstrating different Fn Java FDK features.

#### Configuring your Function
Want to set up function object state before the function is invoked using external configuration variables?  Have a look at the [Function
Configuration](docs/FunctionConfiguration.md) tutorial.

#### Handling HTTP Requests
Want to access HTTP details such as request or response headers or the HTTP status? Check out [Accessing HTTP Information from Functions](docs/HTTPGatewayFunctions.md).

#### Input and Output Bindings
You have the option of taking more control of how serialization and
deserialization is performed by defining your own bindings.

See the [Data Binding](docs/DataBinding.md) tutorial for other out-of-the-box
options and the [Extending Data Binding](docs/ExtendingDataBinding.md) tutorial
for how to define and use your own bindings.

#### Asynchronous Workflows
Suppose you want to call out to some other function from yours - perhaps
a function written in a different language, or even one maintained by
a different team. Maybe you then want to do some processing on the result. Or
even have your function interact asynchronously with a completely different
system. Perhaps you also need to maintain some state for the duration of your
function, but you don't want to pay for execution time while you're waiting for
someone else to do their work.

If this sounds like you, then have a look at the [Fn Flow
Quickstart](docs/FnFlowsUserGuide.md).

