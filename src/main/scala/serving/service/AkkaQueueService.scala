package serving.service

import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{OverflowStrategy, QueueOfferResult}
import serving.akka.AkkaManager._
import serving.cache.AkkaCacheManager.CosineCache
import serving.config.ConfigManager
import serving.model.CosineSimilarity

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object AkkaQueueService {
  private val log = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private type K = String
  private type V = Array[Float]

  private val BUFFER_SIZE: Int = 100000
  private val PROCESS_SIZE: Int = ConfigManager.batch


  private val queue: SourceQueueWithComplete[(K, V)] =
    Source.queue[(K, V)](BUFFER_SIZE, OverflowStrategy.dropHead)
      .groupedWithin(PROCESS_SIZE, 5.millis)
      .toMat(Sink.foreach((x: Seq[(K, V)]) => betch(x)))(Keep.left)
      .run()

  private def betch(inputs: Seq[(K, V)]): Seq[String] = {
    val startTime = System.currentTimeMillis()
    val request = inputs.map(_._2).toArray
    val cos = CosineSimilarity(request)
    val key = inputs.map(x => x._1)
    log.info(s"akka dynamic batching size = ${inputs.length} elapsedTime=${System.currentTimeMillis() - startTime}ms")
    (key zip cos).foreach(x => CosineCache.put(key = x._1, value = x._2))
    key
  }

  def offer(x: (K, V)): Future[QueueOfferResult] = {
    queue.offer(x)
  }

}
