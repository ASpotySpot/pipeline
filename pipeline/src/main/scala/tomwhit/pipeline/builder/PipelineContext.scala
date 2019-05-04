package tomwhit.pipeline.builder

import cats.Parallel
import tomwhit.pipeline.typeclasses.Cache

trait PipelineContext[F[_], G[_]] {
  implicit def par: Parallel[F, G]
  implicit def cache: Cache[F]
}

object PipelineContext {
  implicit def build[F[_], G[_]](implicit Par: Parallel[F, G], C: Cache[F]): PipelineContext[F, G] = new PipelineContext[F, G] {
    override implicit def par: Parallel[F, G] = Par
    override implicit def cache: Cache[F] = C
  }
}
