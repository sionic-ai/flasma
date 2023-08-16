package serving.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

final case class Embedding(embedding: Array[Float])
final case class ResultResponse(result: Array[(Int, Float)])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val responseFormat: RootJsonFormat[ResultResponse] =
    jsonFormat1(ResultResponse)

  implicit val embeddingFormat: RootJsonFormat[Embedding] =
    jsonFormat1(Embedding)

}
