# cascade
Reactive Cascade is an Android library for concurrent reactive programming

Key advantages:

1. crazy fast for great UX
  * everything is concurrent and thread safe by default
  * keep all cores lit while minimizing context swtiching and work in progress
  * CPU-bound tasks are up to 4x faster on a 4 core device
  * file system, network and general CPU tasks never accidentally "leak" onto the UI thread
1. easy to read
  * interfaces are lambda-friendly so you can see the flow of your code in one place
  * a small, consistent library to minimize your learning time
1. easy to write
  * extensive use of generics and runtime assertions
  * just play; checks and useful error messages will guide you
1. easy to debug
  * track asynchronous effects back to their origin with clickable links in the log
1. functional chains
  * composable, immutable object for traditional procedural flow-of-control
  * ideal for use with existing APIs such as Android
1. reactive chains
  * data driven where this makes more sense
  * subscribe concurrent actions to atomic variables 
1. strict and explicit
  * threadgroup for execution is defined by a method, not how it is called
  * binding context is explicitly defined and cleaned
  * minimal boilerplate


Website: [reactivecascade.com](http://reactivecascade.com/)

CI: [Travis](https://travis-ci.org/futurice/cascade)

API: [JavaDoc](http://reactivecascade.com/docs/JavaDoc/)

Other: [Documentation](/docs)

How to use:
Clone and play. This is a pre-release work in progress. We refactor when a better way of doing things becomes clear, so at the moment it is subject to change. 