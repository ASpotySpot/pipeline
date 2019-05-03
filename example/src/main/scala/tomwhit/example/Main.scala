package tomwhit.example

import monix.eval.Task
import monix.eval.instances.CatsParallelForTask
import shapeless.{::, HNil}
import monix.execution.Scheduler.Implicits.global
import tomwhit.example.ExampleComponents.{C, Conf, Written, Z}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val pv: CatsParallelForTask = CatsParallelForTask
    val p = new ExamplePipeline[Task, Task.Par](Conf()).build
    val x = p.evalMany[Task[Option[Written[Z]]] :: Task[Option[Written[C]]] :: HNil].get.runSyncUnsafe()
    println(x)
  }
}
