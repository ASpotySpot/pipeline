---
layout: home
title:  "Home"
section: "home"
---


# Pipeline

The purpose of this library is to allow a processing pipeline to be expressed purely, safely and for it to be completely composable.

## Features
- Composable: Each pipeline can be combined with another such that:
  - One supplies the depencies for the other
  - Their depencies are combined
  - They run completely independently
- Completely typed
  - It is not possible to add a component or pipeline which has unmet depdencies.
- Automatic handling of concurrent execution. 
  - Assuming an effect monad provided supports concurrent execution. based on cats.Parallel
- Execution Plan:
  - It is possible to build a pipeline and only request a single result to be extracted. Only the parts of the pipeline requested will be executed

### Detail
There are 2 primary abstractions at use:
 
- Component: This is simply a wrapper around a function `I => F[O]`
- Pipeline: A State monad backed by a HList of all values computed so far. 

The methods used in the example search (at compile time) for the required inputs in the HList and uses that to run the component.
The result of the component is then prepended onto the HList to allow it to be used by other computations.

