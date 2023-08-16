package serving.http

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.ServiceUnavailable
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import serving.akka.AkkaManager._

import serving.service.CosineSimilarityService

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

class CosineSimilarityRoute extends JsonSupport {
  private val log = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def serviceUnavailable: StandardRoute =
    complete(
      HttpResponse(ServiceUnavailable,
        entity = "The server is currently unavailable (because it is overloaded or down for maintenance).")
    )

  val route: Route = CosRoute()

  private def CosRoute(): Route =
    pathPrefix("cos") {
      entity(as[Embedding]) { emb =>

        val scoreF: Future[Option[Array[(Int, Float)]]] = CosineSimilarityService(emb)

        val resultF: Future[StandardRoute] = scoreF.map {
          case Some(prob) =>
            complete(ResultResponse(prob))
          case None =>
            log.error(s"503 serviceUnavailable : Empty result")
            serviceUnavailable
        }

        onComplete(resultF) {
          case Success(success) =>
            success
          case Failure(f) =>
            log.error(s"503 serviceUnavailable : $f")
            serviceUnavailable
        }

      }
    }
}
