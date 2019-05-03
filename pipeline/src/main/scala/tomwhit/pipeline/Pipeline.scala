package tomwhit.pipeline

import cats.data.{IndexedStateT, StateT}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, Monad, Parallel}
import shapeless.ops.hlist.{Prepend, SelectAll, Selector, Tupler}
import shapeless.{::, HList, HNil}
import tomwhit.pipeline.typeclasses.{Cache, Producter, Threader}
import tomwhit.pipeline.typeclasses.Cache._

class Pipeline[F[_], G[_], Q <: HList, T <: HList, H] private(private val ids: IndexedStateT[F, Q, T, F[H]])(implicit Par: Parallel[F, G], C: Cache[F]) {
  self =>

  //Instances
  implicit def M: Monad[F] = Par.monad
  implicit def G: Applicative[G] = Par.applicative
  type Aux[S <: HList, A] = Pipeline[F, G, Q, S, A]

  //Basic Constructors
  def from[S <: HList, A](newIds: IndexedStateT[F, Q, S, F[A]]): Aux[S, A] = new Pipeline[F, G, Q, S, A](newIds)
  def transform[S <: HList, A](f: (T, F[H]) => (S, F[A])): Aux[S, A] = {
    val newIds = self.ids.transform { case (t, fh) =>
      val (s, fa) = f(t, fh)
      (s, fa.memoize)
    }
    from(newIds)
  }

  //Basic Transforms
  def put: Aux[F[H] :: T, H] = transform { case (s, fa) => (fa :: s, fa) }
  def retrieve[B](implicit S: Selector[T, F[B]]): Aux[T, B] = transform { case (s, _) => (s, S(s)) }
  def refine[S <: HList](implicit S: SelectAll[T, S]): Aux[S.Out, H] = transform { case (t, fh) => (S(t), fh) }

  def map[B](f: H => B): Aux[T, B] = transform { case (s, fa) => (s, fa.map(f))}
  def mapP[B](f: H => B): Aux[F[B] :: T, B] = transform { case (s, fa) =>
    val fb = fa.map(f)
    (fb :: s, fb)
  }

  def flatMap[B](f: H => F[B]): Aux[T, B] = transform { case (s, fa) => (s, fa.flatMap(f)) }
  def flatMapP[B](f: H => F[B]): Aux[F[B] :: T, B] = transform { case (s, fa) =>
    val fb = fa.flatMap(f)
    (fb :: s, fb)
  }

  def add[B](b: B): Aux[F[B] :: T, B] = addF(M.pure(b))
  def addF[B](fb: F[B]): Aux[F[B] :: T, B] = transform { case (s, _) =>
    (fb :: s, fb)
  }


  //Component & Pipeline Methods
  def component[X, B](c: Component[F, X, B])(implicit S: Selector[T, F[X]]): Aux[F[B] :: T, B] = retrieve(S).flatMapP(x => c.apply(x))

  def pipelineDiscard[B](p2: Aux[_ <: HList, B])(implicit ev: HNil =:= Q): Aux[F[B] :: T, B] = {
    addF(p2.ids.runA(HNil).flatten)
  }

  def pipeline[T2 <: HList, B](p2: Aux[T2, B])(implicit ev: HNil =:= Q, Pre: Prepend[T2, T]): Aux[F[B] :: Pre.Out, B] = {
    val newIds = ids.flatMapF { _ => p2.ids.run(HNil) }.transform { case (t, (t2, fb)) => (fb :: Pre(t2, t), fb) }
    from(newIds)
  }

  //Combinators
  def joinF[B1, B2](implicit
                    S: SelectAll[T, F[B1] :: F[B2] :: HNil],
                    T: Tupler.Aux[F[B1] :: F[B2] :: HNil, (F[B1], F[B2])]): Aux[F[(B1, B2)] :: T, (B1, B2)] = {
    transform[T, (B1, B2)] { case (s, _) =>
      val (fb1, fb2): (F[B1], F[B2]) = T(S(s))
      val fb1b2: F[(B1, B2)] = Parallel.parProduct[F, G, B1, B2](fb1, fb2)
      s -> fb1b2
    }.put
  }

  //Evaluators
  def eval[L](implicit ev: HNil =:= Q, S: Selector[T, F[L]]): F[L] = {
    ids.transform { case (t, _) => ((), S(t)) }.runA(HNil).flatten
  }
  def complete(implicit ev: HNil =:= Q): F[T] = ids.runS(HNil)

  class EvalManyHelper[L <: HList](implicit S: SelectAll[T, L], ev: HNil =:= Q) {
    def get(implicit P: Producter[F, L]): P.Out = ids.runS(HNil).map(S.apply).flatMap(t => P(t))
  }

  def evalMany[L <: HList](implicit S: SelectAll[T, L], ev: HNil =:= Q): EvalManyHelper[S.Out] = {
    new EvalManyHelper[S.Out]
  }


  //Conditional Operators
  def getOrElse[X, B](c: Component[F, HNil, B])(implicit S: Selector[T, F[Option[B]]]): Aux[F[B] :: T, B] = retrieve(S).flatMapP {
    case Some(b) => M.pure(b)
    case None => c()
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
  private[pipeline] def apply[F[_], G[_]](implicit P: Parallel[F, G], C: Cache[F]): Pipeline[F, G, HNil, HNil, HNil] =
    new Pipeline[F, G, HNil, HNil, HNil](StateT.pure[F, HNil, F[HNil]](P.monad.pure(HNil))(P.monad))
}

