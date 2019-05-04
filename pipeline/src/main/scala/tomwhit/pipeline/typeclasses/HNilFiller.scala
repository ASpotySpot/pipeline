package tomwhit.pipeline.typeclasses

import cats.{Applicative, Monad, Traverse}
import shapeless.{::, DepFn0, HList, HNil}

trait HNilFiller[L <: HList] extends DepFn0 with Serializable {self =>
  type Out <: HList
  override def apply(): Out
}

object HNilFiller {
  type Aux[L <: HList] = HNilFiller[L] {type Out = L}

  def apply[L <: HList](implicit F: HNilFiller.Aux[L]): HNilFiller.Aux[L] = F

  implicit val hnil: HNilFiller.Aux[HNil] = new HNilFiller[HNil] {
    override type Out = HNil
    override def apply(): HNil = HNil
  }

  implicit def hlist[L <: HList](implicit HNF: HNilFiller.Aux[L]): HNilFiller.Aux[HNil :: L] = new HNilFiller[HNil :: L] {
    override type Out = HNil :: L
    override def apply(): HNil :: L = HNil :: HNF.apply()
  }
}

object TestMain {
  val x: HNilFiller.Aux[HNil :: HNil] = HNilFiller[HNil :: HNil]
}