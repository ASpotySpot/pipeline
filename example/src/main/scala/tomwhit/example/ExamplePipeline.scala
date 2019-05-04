package tomwhit.example

import tomwhit.example.ExampleComponents._
import tomwhit.pipeline.builder._

class ExamplePipeline[F[_], G[_]](c: Conf)(implicit P: PipelineContext[F, G]) extends Builder[F, G] {
  def build = {

    /**
      * This section of the pipeline 'Reads' an A (It just exposes the instance passed into the constuctor for examples sake)
      * It then adds a component which converts an A to a B
      * It then adds a component which converts an B to a C
      * It then writes down this C only if the configuration specifies it
      * It then states to only do all of this if the configuration specifies it or else it falls back to providing a C by reading it
      */
    val makeC = {
      pipeline.
        component(Read(A("Reading A"))).
        component(AToB()).
        component(BToC()).
        component(Write[F, C]().onlyIf(c.writeC)).
        onlyIf(c.readA).
        getOrElse(Read(C("Reading C")))
    }

    val makeY = {
      /**
        * This section of the pipeline performs the same as the above but instead the flow is
        * Read X -> Convert X to Y -> (Write Y Only if Configure)
        * Only do all of above if configured to do so or else Read Y
        */
      pipeline.
        component(Read(X("Reading X"))).
        component(XToY()).
        component(Write[F, Y]().onlyIf(c.writeY)).
        onlyIf(c.readX).
        getOrElse(Read(Y("Reading Y")))
    }

    /**
      * This sections shows one of the simplest ways of composing multiple pipelines
      * Start with an empty pipeline then add the makeC pipeline.
      * Then add the makeY pipeline.
      * Take a C and a Y from previously calculating results and combine them into a single tuple
      *   This is done in parallel
      * Add a component that converts (C, Y) -> Z
      * Then write Z only if configured to do so.
      */

    pipeline.
      pipeline(makeC).
      pipeline(makeY).
      joinF[C, Y].
      component(CYToZ()).
      component(Write[F, Z].onlyIf(c.writeZ))
  }
}

