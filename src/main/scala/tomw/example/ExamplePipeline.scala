package tomw.example

import cats.Parallel
import tomw.example.ExampleComponents._
import tomw.pipeline.{Builder, Cache}

class ExamplePipeline[F[_], G[_]](c: Conf)(implicit P: Parallel[F, G], Ca: Cache[F]) extends Builder[F, G] {

  def build = {
    def makeC = {
      pipeline.
        component(Read(A("Reading A"))).
        component(AToB()).
        component(BToC()).
        component(Write[F, C]().onlyIf(c.writeC)).
        onlyIf(c.readA).
        getOrElse(Read(C("Reading C")))
    }

    def makeY = {
      pipeline.
        component(Read(X("Reading X"))).
        component(XToY()).
        component(Write[F, Y]().onlyIf(c.writeY)).
        onlyIf(c.readX).
        getOrElse(Read(Y("Reading Y")))
    }

    pipeline.
      pipeline(makeC).
      pipeline(makeY).
      joinF[C, Y].
      component(CYToZ()).
      component(Write[F, Z]).
      onlyIf(c.writeZ)
  }
}

