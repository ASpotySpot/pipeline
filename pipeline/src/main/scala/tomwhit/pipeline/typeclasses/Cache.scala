package tomwhit.pipeline.typeclasses


trait Cache[F[_]] {
  def memoize[A](fa: F[A]): F[A]
}

object Cache {
  implicit class CacheOps[F[_], A](fa: F[A])(implicit C: Cache[F]) {
    def memoize: F[A] = C.memoize(fa)
  }
}
