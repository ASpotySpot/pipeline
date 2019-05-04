package tomwhit.example

import cats.Monad
import shapeless.HNil
import tomwhit.pipeline.Component
import cats.syntax.flatMap._

object ExampleComponents {
  case class Conf(readA: Boolean = true,
                  readX: Boolean = true,
                  writeC: Boolean = true,
                  writeY: Boolean = true,
                  writeZ: Boolean = true)

  case class A(value: String)
  case class B(value: String)
  case class C(value: String)
  case class X(value: String)
  case class Y(value: String)
  case class Z(value: String)

  val sleepTime: Long = 100l
  case class AToB[F[_]]()(implicit protected val  F: Monad[F]) extends BaseComponent[F, A, B] {
    override protected def run(i: A): F[B] = F.pure{Thread.sleep(sleepTime); B(s"from $i")}
    override def id: String = "AtoB"
  }
  case class BToC[F[_]]()(implicit protected val  F: Monad[F]) extends BaseComponent[F, B, C] {
    override protected def run(i: B): F[C] = F.pure{Thread.sleep(sleepTime); C(s"from $i")}
    override def id: String = "BtoC"
  }
  case class XToY[F[_]]()(implicit protected val  F: Monad[F]) extends BaseComponent[F, X, Y] {
    override protected def run(i: X): F[Y] = F.pure{Thread.sleep(sleepTime); Y(s"from $i")}
    override def id: String = "XtoY"
  }
  case class CYToZ[F[_]]()(implicit protected val F: Monad[F]) extends BaseComponent[F, (C, Y), Z] {
    override protected def run(i: (C, Y)): F[Z] = F.pure{Thread.sleep(sleepTime); Z(s"from $i")}
    override def id: String = "C+YtoZ"
  }
  case class Read[F[_], T](t: T)(implicit protected val  F: Monad[F]) extends BaseComponent[F, HNil, T] {
    override protected def run(i: HNil): F[T] = F.pure(t)
    override def id: String = t.toString
  }
  case class Write[F[_], T]()(implicit protected val  F: Monad[F]) extends BaseComponent[F, T, Written[T]] {
    override protected def run(i: T): F[Written[T]] = F.pure{println(s"Writing($i)");new Written[T]}
    override def id: String = ""
  }

  class Written[Q]

  trait BaseComponent[F[_], I, O] extends LoggingComponent[F, I, O] with MetricComponent[F, I, O]

  trait LoggingComponent[F[_], I, O] extends Component[F, I, O] {
    protected def id: String
    override protected def before: F[Unit] = F.pure(println(s"Starting [${System.currentTimeMillis() / 1000}] ${getClass.getName}($id)")) >> super.before
    override protected def after: F[Unit] = F.pure(println(s"Finished [${System.currentTimeMillis() / 1000}] ${getClass.getName}($id)")) >> super.after
  }

  trait MetricComponent[F[_], I, O] extends Component[F, I, O] {
    protected def id: String
    protected def pushMetric(name: String, l: Long): Unit = {} //println(s"Pushing Value ($l, $name) for $id")
    override protected def before: F[Unit] = F.pure(pushMetric("start_time", System.currentTimeMillis())) >> super.before
    override protected def after: F[Unit] = F.pure(pushMetric("end_time", System.currentTimeMillis())) >> super.after
  }
}
