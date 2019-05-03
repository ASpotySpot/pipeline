package tomwhit.pipeline

import cats.instances.{OptionInstances, OptionInstancesBinCompat0}
import cats.{Applicative, Monad, Parallel}
import shapeless.HNil
import shapeless.ops.hlist.Selector
import tomwhit.pipeline.typeclasses.{Cache, ThreaderInstances}

abstract class Builder[F[_], G[_]](implicit Par: Parallel[F, G], C: Cache[F]) extends BuilderImplicits {
  implicit val M: Monad[F] = Par.monad
  implicit val G: Applicative[G] = Par.applicative

  protected def pipeline: Pipeline[F, G, HNil, HNil] = Pipeline[F, G]
}

trait BuilderImplicits
  extends OptionInstances with OptionInstancesBinCompat0
  with ThreaderInstances {
  implicit def liftSel[F[_]](implicit F: Applicative[F]): Selector[HNil, F[HNil]] = {
    new Selector[HNil, F[HNil]] {
      override def apply(t: HNil): F[HNil] = F.pure(t)
    }
  }
}
