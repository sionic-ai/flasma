package serving.http

import akka.http.scaladsl.Http
import serving.akka.AkkaManager._
import serving.config.ConfigManager
import serving.model.CosineSimilarity

object HttpServer extends JsonSupport {
  private val log = org.slf4j.LoggerFactory.getLogger(this.getClass)
  private val HTTP_PORT: Int = ConfigManager.port

  def main(args: Array[String]): Unit = {
    val cosineSimilarityRoute = new CosineSimilarityRoute()
    val routes = cosineSimilarityRoute.route
    log.info(s"Warmup start")
    val warmup = CosineSimilarity(Array(Array.fill[Float](ConfigManager.dim)(1.0f)))

    val bindingFuture = Http().newServerAt("0.0.0.0", HTTP_PORT).bind(routes)
    bindingFuture.onComplete {
      case scala.util.Success(s) =>
        log.info(s"Server now online. $s")

      case scala.util.Failure(f) =>
        f.printStackTrace()
        log.error(s"Bind server error=${f.getMessage}")
    }
  }
}
