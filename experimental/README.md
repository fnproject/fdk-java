#Experimental API Features

This module contains features of the API that are not, at present, release-ready enough to be added to the API module, 
but have enough useful functionality that they are exposed here nonetheless.

All features, while tested, are not guaranteed to be safe and should be used with caution; production use is not yet 
recommended for these features. Breaking, non-backwards compatible, changes to these features are likely and should be expected.

##Retry
The **_Retry_** package, used with **Fn _Flows_**, allows for _FlowFutures_ to be retried upon exception, using configurable retry 
options - consisting of a _delay strategy_ and a _retry strategy_. For users who do not wish to configure their own retry behaviours,
some default implementations of the interfaces have been provided. See below and the [Javadocs]() for more detail.

####The _retry_ method
The default method in the package is _retry_ (which is just a synonym for _retryExponentialWithJitter_). It takes a _Flows.SerCallable<T>_
operation, and repeatedly tries it at random, increasing intervals - determined by its _DelayStrategy_ - until it determines that it must
stop retrying, according to its _StopStrategy_.

#####DelayStrategy
This interface provides one method - _delay()_ - which must return the maximum amount of time in milliseconds to wait before retrying the operation.

#####StopStrategy
This interface provides one method - _shouldRetry_ - which returns whether or not the operation should be retried, as a function of:
the exception raised by the operation to be retried on its previous attempt; the attempt number; the total time in milliseconds that
has expired since the first attempt failed.

####Example Usage and provided/default implementations
We provide default implementations of the _DelayStrategy_ interface as follows:
* _ExponentialDelayStrategy_ exponentially backs off, but each delay can only have a maximum wait equal to the _delayMaxDuration_ field.
* _FibonacciDelayStrategy_ increases the delay according to the fibonacci sequence, but each delay can only be as long as _delayMaxDuration_.

We also provide default implementations of the _StopStrategy_ interface as follows:
* _MaxAttemptStopStrategy_ stops if more than _maxAttempts_ retries have been attempted.
* _TimeoutStopStrategy_ stops if more than _timeout_ time has passed since the first retry.

So far we provide the following methods as a part of the API:
* _retryExponentialWithJitter_ - uses exponential backoff and a max-attempt-based strategy.
* _retryFibonacciWithJitter_ - uses fibonacci backoff and a max-attempt-based-strategy.
* _retryWithOpts_ - uses the provided retryOpts object to decide strategy.

