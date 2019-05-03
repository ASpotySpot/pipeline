package tomwhit.pipeline.typeclasses

import monix.eval.Task

trait Cache[F[_]] {
  def memoize[A](fa: F[A]): F[A]
}

object Cache {
  implicit val taskCache: Cache[Task] = new Cache[Task] {
    override def memoize[A](fa: Task[A]): Task[A] = fa.memoize
  }

  implicit class CacheOps[F[_], A](fa: F[A])(implicit C: Cache[F]) {
    def memoize: F[A] = C.memoize(fa)
  }
}
