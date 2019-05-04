package tomwhit.example

import cats.Parallel
import shapeless.{::, HNil}
import tomwhit.example.ExampleComponents._
import tomwhit.pipeline.builder.Builder
import tomwhit.pipeline.typeclasses.Cache

class ExampleFragment[F[_], G[_]](c: Conf)(implicit P: Parallel[F, G], Ca: Cache[F]) extends Builder[F, G] {

  def build = {
    val joiner = fragment[F[C] :: F[Y] :: HNil].
      joinF[C, Y].
      component(CYToZ()).
      component(Write[F, Z]).
      onlyIf(c.writeZ)

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
      pipeline.
        component(Read(X("Reading X"))).
        component(XToY()).
        component(Write[F, Y]().onlyIf(c.writeY)).
        onlyIf(c.readX).
        getOrElse(Read(Y("Reading Y")))
    }

    val cy = makeC.joinDep(makeY)
    joiner.prepend(cy)
  }
}

