package tomwhit.pipeline.typeclasses

import cats.{Monad, Parallel}
import shapeless.{::, DepFn1, HList, HNil}

trait Producter[F[_], L] extends DepFn1[L] with Serializable {
  self =>
  type Out = F[Out2]
  type Out2 <: HList

  override def apply(t: L): Out
}

object Producter {
  type Aux[F[_], L, Out0] = Producter[F, L] {type Out2 = Out0}

  def lift[F[_], L](l: L)(implicit F: Producter[F, L]): F.Out = F(l)

  def apply[F[_], L](implicit F: Producter[F, L]): Producter[F, L] = F

  implicit def hnil[F[_]](implicit F: Monad[F]): Aux[F, HNil, HNil] = new Producter[F, HNil] {
    override type Out2 = HNil
    override def apply(t: HNil): F[HNil] = F.pure(t)
  }

  implicit def hlist[F[_], G[_], H, T <: HList](implicit
                                                Par: Parallel[F, G],
                                                TF: Producter[F, T]): Aux[F, F[H] :: T, H :: TF.Out2] =
    new Producter[F, F[H] :: T] {
      override type Out2 = H :: TF.Out2

      override def apply(t: F[H] :: T): F[H :: TF.Out2] = {
        val pt = Parallel.parProduct[F, G, H, TF.Out2](t.head, TF(t.tail))
        Par.monad.map(pt){case (h, t) => h :: t}
      }
    }
}