package serving.akka

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.ExecutionContextExecutor

object AkkaManager {
  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "akka-dynamic-batch-tensorflow-gpu-graph-cosine-similarity")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
}
