package tomw.pipeline

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.Monad
import shapeless.HNil

trait Component[F[_], I, O] {self =>
  protected implicit def F: Monad[F]
  protected def before: F[Unit] = F.pure(())
  protected def after: F[Unit] = F.pure(())
  protected def run(i: I): F[O]

  def apply(i: I): F[O] = for {
    _ <- before
    o <- run(i)
    _ <- after
  } yield o

  def apply()(implicit ev: HNil =:= I): F[O] = apply(HNil)
  def onlyIf(b: Boolean): Component[F, I, Option[O]] = new Component[F, I, Option[O]] {
    override implicit protected def F: Monad[F] = self.F
    override protected def before: F[Unit] = if(b) self.before else F.pure(())
    override protected def after: F[Unit] = if(b) self.after else F.pure(())
    override protected def run(i: I): F[Option[O]] = if(b) self.run(i).map(Some(_)) else F.pure(None)
  }

  def getOrRun[O2](c2: Component[F, I, O2])(implicit ev: O <:< Option[O2]): Component[F, I, O2] = new Component[F, I, O2] {
    override implicit protected def F: Monad[F] = self.F
    override protected def before: F[Unit] = c2.before
    override protected def after: F[Unit] = c2.after
    override protected def run(i: I): F[O2] = for {
      o <- self.run(i)
      o2 <- ev(o) match {
        case Some(o2) => F.pure(o2)
        case None => c2.apply(i)
      }
    } yield o2
  }
}
