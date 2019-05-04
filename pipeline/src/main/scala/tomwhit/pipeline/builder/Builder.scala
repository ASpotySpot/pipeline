package tomwhit.pipeline.builder

import cats.instances.{OptionInstances, OptionInstancesBinCompat0}
import cats.{Applicative, Monad, Parallel}
import shapeless.ops.hlist.Selector
import shapeless.{HList, HNil}
import tomwhit.pipeline.Pipeline
import tomwhit.pipeline.typeclasses.{Cache, ThreaderInstances}

abstract class Builder[F[_], G[_]](implicit Ctx: PipelineContext[F, G]) extends BuilderImplicits {
  implicit val Par: Parallel[F, G] = Ctx.par
  implicit val Cache: Cache[F] = Ctx.cache
  implicit val M: Monad[F] = Ctx.par.monad
  implicit val G: Applicative[G] = Ctx.par.applicative

  protected def pipeline: Pipeline[F, G, HNil, HNil, HNil] = Pipeline[F, G, HNil]
  protected def fragment[Q <: HList]: Pipeline[F, G, Q, Q, HNil] = Pipeline[F, G, Q]
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
