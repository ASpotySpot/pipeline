package tomwhit

import cats.data.{IndexedStateT, StateT}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, Monad, Parallel}
import shapeless.{::, HList, HNil}
import shapeless.ops.hlist.{Prepend, SelectAll, Selector, Tupler}
import tomwhit.pipeline.Cache._
import tomwhit.pipeline.Component

class Pipeline[F[_], G[_], T <: HList, H] private(private val ids: IndexedStateT[F, HNil, T, F[H]])(implicit Par: Parallel[F, G], C: Cache[F]) {
  self =>

  implicit def M: Monad[F] = Par.monad

  implicit def G: Applicative[G] = Par.applicative

  type Aux[S <: HList, A] = Pipeline[F, G, S, A]

  def from[S <: HList, A](newIds: IndexedStateT[F, HNil, S, F[A]]): Aux[S, A] = new Pipeline[F, G, S, A](newIds)

  def transform[S <: HList, A](f: (T, F[H]) => (S, F[A])): Aux[S, A] = {
    val newIds = self.ids.transform { case (t, fh) =>
      cache(f(t, fh))
    }
    from(newIds)
  }

  private def cache[S, B](sfh: (S, F[B])): (S, F[B]) = (sfh._1, sfh._2.memoize)

  def put: Aux[F[H] :: T, H] = transform { case (s, fa) => (fa :: s, fa) }

  def map[B](f: H => B): Aux[F[B] :: T, B] = transform { case (s, fa) =>
    val fb = fa.map(f)
    (fb :: s, fb)
  }

  def flatMap[B](f: H => F[B]): Aux[F[B] :: T, B] = transform { case (s, fa) =>
    val fb = fa.flatMap(f)
    (fb :: s, fb)
  }

  def add[B](b: B): Aux[F[B] :: T, B] = addF(M.pure(b))

  def addF[B](fb: F[B]): Aux[F[B] :: T, B] = transform { case (s, _) =>
    (fb :: s, fb)
  }


  def retrieve[B](implicit S: Selector[T, F[B]]): Aux[T, B] = transform { case (s, _) => (s, S(s)) }

  def component[X, B](c: Component[F, X, B])(implicit S: Selector[T, F[X]]): Aux[F[B] :: T, B] = retrieve(S).flatMap(x => c.apply(x))

  def pipelineDiscard[B](p2: Aux[_ <: HList, B]): Aux[F[B] :: T, B] = {
    addF(p2.ids.runA(HNil).flatten)
  }

  def pipeline[T2 <: HList, B](p2: Aux[T2, B])(implicit Pre: Prepend[T2, T]): Aux[F[B] :: Pre.Out, B] = {
    val newIds = ids.flatMapF { _ => p2.ids.run(HNil) }.transform { case (t, (t2, fb)) => (fb :: Pre(t2, t), fb) }
    from(newIds)
  }

  def getOrElse[X, B](c: Component[F, HNil, B])(implicit S: Selector[T, F[Option[B]]]): Aux[F[B] :: T, B] = retrieve(S).flatMap {
    case Some(b) => M.pure(b)
    case None => c()
  }

  def joinF[B1, B2](implicit
                    S: SelectAll[T, F[B1] :: F[B2] :: HNil],
                    T: Tupler.Aux[F[B1] :: F[B2] :: HNil, (F[B1], F[B2])]): Aux[F[(B1, B2)] :: T, (B1, B2)] = {
    transform[T, (B1, B2)] { case (s, _) =>
      val (fb1, fb2): (F[B1], F[B2]) = T(S(s))
      val fb1b2: F[(B1, B2)] = Parallel.parProduct[F, G, B1, B2](fb1, fb2)
      s -> fb1b2
    }.put
  }

  def eval[L](implicit S: Selector[T, F[L]]): F[L] = {
    ids.transform { case (t, _) => ((), S(t)) }.runA(HNil).flatten
  }

  def refine[S <: HList](implicit S: SelectAll[T, S]): Aux[S.Out, H] = transform { case (t, fh) => (S(t), fh) }

  def complete: F[T] = ids.runS(HNil)

  class EvalManyHelper[L <: HList](implicit S: SelectAll[T, L]) {
    def get(implicit P: Producter[F, L]): P.Out = ids.runS(HNil).map(S.apply).flatMap(t => P(t))
  }

  def evalMany[L <: HList](implicit S: SelectAll[T, L]): EvalManyHelper[S.Out] = {
    new EvalManyHelper[S.Out]
  }

  def onlyIf(b: Boolean)(implicit L: Threader[F, Option, T]): Aux[L.Out, Option[H]] = {
    if (b) {
      self.transform { case (t, fh) =>
        (L(Some(t)), fh.map(Some.apply))
      }
    } else {
      self.transform { case (_, _) =>
        (L(None), M.pure(None))
      }
    }
  }
}


object Pipeline {
  private[pipeline] def apply[F[_], G[_]](implicit P: Parallel[F, G], C: Cache[F]): Pipeline[F, G, HNil, HNil] =
    new Pipeline[F, G, HNil, HNil](StateT.pure[F, HNil, F[HNil]](P.monad.pure(HNil))(P.monad))
}

