---
layout: home
title:  "Home"
section: "home"
---


# Pipeline

The purpose of this library is to allow a processing pipeline to be expressed purely, safely and for it to be completely composable.
It hides all the building blocks behind dsl-like syntax. 


### Detail
There are 2 primary abstractions at use:
 
- Component: This is simply a wrapper around a function `I => F[O]`
- Pipeline: A State monad backed by a HList of all values computed so far. 

The methods used in the example search (at compile time) for the required inputs in the HList and uses that to run the component.
The result of the component is then prepended onto the HList to allow it to be used by other computations.

