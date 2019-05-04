---
layout: docs
title:  "Composition"
section: "docs"
---

### Why not just chain a sequence of functions

An alternative attempt at an approve would be to just chain some functions.
```scala mdoc
import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._

def pipeline[F[_]: Monad] = {
  def component0(): F[Int] = ???
  def component1(i: Int): F[String] = ???
  def component2(i: String): F[Double] = ???

  for {
    a <- component0()
    b <- component1(a)
    c <- component2(b)
  } yield c
}
```

However suppose I know want to pass this 'pipeline' to some other process that requires both the a and c.
In fact all abc may need to be accessed downstream. So the yield then has to change to `yield(a,b,c)`

This however forces evaluation of c to happen even if the downstream process needs a `b`.
The following approach solves these problems.
```scala mdoc
def pipeline2[F[_]](implicit F: Monad[F]) = {
  def component0(): F[Int] = F.pure({println("0"); 0})
  def component1(i: Int): F[String] = F.pure({println("1"); "1"})
  def component2(i: String): F[Double] = F.pure({println("2"); 2.0})

  val fpipeline: F[(F[Int], F[String], F[Double])] = for {
    fa <- F.pure(component0())
    fb <- fa.map(component1)
    fc <- fb.map(component2)
  } yield (fa, fb, fc)
  
  //These 4 lines can be solved with some 'shapeless magic' 
  //see tomwhit.pipelines.typeclasses.Threader
  val f1 = fpipeline.flatMap(_._1)
  val f2 = fpipeline.flatMap(_._2)
  val f3 = fpipeline.flatMap(_._3)
  (f1, f2, f3)
}
```

Other problems also still exist:
- Flattenting ut all the different result tuples that appear 
- Different pipeline routing based on config
- Handling independent/parallel execution

All these can be solved but then we end up with a solution very similar to the Pipeline.

