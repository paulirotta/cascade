# cascade
Reactive Cascade is an Android library for concurrent programming

Key advantages:

1. high performance for great UX
  * everything is concurrent by default; serial execution is available
  * keep all cores lit while minimizing context swtiching and work in progress
  * CPU-bound tasks are up to Nx faster on a N core device
  * file system, network and general CPU tasks never accidentally "leak" onto the UI thread
1. strict and explicit
  * threadgroup for execution is defined by a method, not how it is called
  * code binding context is always explicitly defined and cleaned
  * minimal boilerplate
1. easy to read
  * small, consistent library to minimize learning time
  * minimal glue objects; lambda-friendly interfaces help see the complete flow of code in one place
1. easy to write
  * just play; most tasks are easy when everything is thread safe
  * clear, explicit contracts; extensive use of generics and runtime assertions
  * easily configure the runtime builder and substitute alternate implementations and default behaviors
1. easy to debug
  * track asynchronous effects back to their origin with clickable links in the log
  * error messages guide you back to where the error was induced by showing object creation and async invocation points

You can freely use the flow-of-control style which makes the most sense

1. functional chains
  * composable, immutable object chains for traditional procedural code
  * ideal for use with existing APIs such as Android
  * objects become immutable value holders once executed
1. reactive chains
  * data driven where this makes more sense
  * subscribe concurrent actions to atomic variables 
  * objects representing intermediate functions in a chain are re-used during concurrent execution

Website: [reactivecascade.com](http://reactivecascade.com/)

CI: [Travis](https://travis-ci.org/futurice/cascade)

API: [JavaDoc](http://reactivecascade.com/docs/JavaDoc/)

Other: [Documentation](/docs)

How to use:
Clone and play. This is a pre-release work in progress. We refactor when a better way of doing things becomes clear. At the moment it is subject to change.
