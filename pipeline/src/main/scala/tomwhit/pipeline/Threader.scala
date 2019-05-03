package tomwhit.pipeline

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, Monad, Traverse}
import shapeless.{::, DepFn1, HList, HNil, Lazy}
import tomwhit.pipeline.Threader.Aux

trait Threader[F[_], G[_], L] extends DepFn1[G[L]] with Serializable {self =>
  type Out <: HList
  override def apply(t: G[L]): Out
}

object Threader extends ThreaderInstances

trait ThreaderInstances extends BasicThread {
  type Aux[F[_], G[_], L, Out0] = Threader[F, G, L] {type Out = Out0}

  def lift[F[_], G[_], L](l: G[L])(implicit F: Threader[F, G, L]): F.Out = F(l)
  def apply[F[_], G[_], L](implicit F: Threader[F, G, L]): Threader[F, G, L] = F

  implicit def hnil[F[_], G[_]]: Aux[F, G, HNil, HNil] = new Threader[F, G, HNil] {
    override type Out = HNil
    override def apply(t: G[HNil]): HNil = HNil
  }

  implicit def flattenThread[F[_], G[_], H, T <: HList](implicit
                                                G: Traverse[G],
                                                M: Monad[G],
                                                F: Applicative[F],
                                                TF: Threader[F, G, T]): Aux[F, G, F[G[H]] :: T, F[G[H]] :: TF.Out] =
    new Threader[F, G, F[G[H]] :: T] {
      override type Out = F[G[H]] :: TF.Out

      override def apply(t: G[F[G[H]] :: T]): F[G[H]] :: TF.Out = {
        val fgh = G.traverse[F, F[G[H]] :: T, G[H]](t)(_.head).map(_.flatten)
        fgh :: TF.apply(G.map(t)(_.tail))
      }
    }


}

trait BasicThread {
  implicit def hlist[F[_], G[_], H, T <: HList](implicit
                                                G: Traverse[G],
                                                F: Applicative[F],
                                                TF: Lazy[Threader[F, G, T]]): Aux[F, G, F[H] :: T, F[G[H]] :: TF.value.Out] =
    new Threader[F, G, F[H] :: T] {
      override type Out = F[G[H]] :: TF.value.Out

      override def apply(t: G[F[H] :: T]): F[G[H]] :: TF.value.Out = {
        val fgh = G.traverse[F, F[H] :: T, H](t)(_.head)
        fgh :: TF.value.apply(t.map(_.tail))
      }
    }
}