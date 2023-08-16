package serving.cache

import scala.concurrent.duration.DurationInt

object AkkaCacheManager {
  val CosineCache: AkkaCache[Array[(Int,Float)]] =
    new AkkaCache[Array[(Int,Float)]]("BatchCache", 100000, 10.minutes, 5.minutes)
}
