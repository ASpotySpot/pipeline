---
layout: docs
title:  "Components"
section: "docs"
---

### Components

The following is a simple component that converts an Int to a String
```scala mdoc
import tomwhit.pipeline.Component
import cats.Monad

class X[F[_]](implicit protected val F: Monad[F]) extends Component[F, Int, String] {
  override protected def run(i: Int): F[String] = F.pure(s"String of $i")
}
```

A component has 2 abstract methods:
- F: `Monad[F]`: This should just be left for the pipeline to pass in
- run: `I => F[O]`: This is the method that does the work

And two overridable methods
- before: `F[Unit]`: This method is called before run
- after: `F[Unit]`: This method is called after run

These 2 methods can be used for things such as logging or metrics updating.

It is possible to 'stack' multiple before and after methods.
```scala mdoc
import cats.syntax.flatMap._
import cats.Id
 
trait LoggingComponent[F[_], I, O] extends Component[F, I, O] {
  override def before: F[Unit] = F.pure(println("Starting")) >> super.before
  override def after: F[Unit] = F.pure(println("Finishing")) >> super.after
}
trait MetricComponent[F[_], I, O] extends Component[F, I, O] {
  override def before: F[Unit] = F.pure(println("Pushing Start Time")) >> super.before
  override def after: F[Unit] = F.pure(println("Pushing Finish Time")) >> super.after
}
trait ReportCompleteComponent[F[_], I, O] extends Component[F, I, O] {
  override def after: F[Unit] = F.pure(println("Reporting Finish")) >> super.after
}
trait BaseComponent[F[_], I, O] 
  extends LoggingComponent[F, I, O] 
  with MetricComponent[F, I, O] 
  with ReportCompleteComponent[F, I, O] 

class Y[F[_]](implicit protected val F: Monad[F]) extends BaseComponent[F, Int, String] {
  override protected def run(i: Int): F[String] = F.pure{Thread.sleep(100); s"String of $i"}
}
new Y[Id].apply(3)

```


