package serving.cache

import serving.akka.AkkaManager._
import akka.http.caching.scaladsl.{Cache, CachingSettings, LfuCacheSettings}
import akka.http.caching.LfuCache
import serving.config.ConfigManager

import scala.concurrent.duration.Duration
import scala.concurrent.Future

class AkkaCache[V <: Any](name: String,
                          size: Int,
                          newTimeToLive: Duration,
                          newTimeToIdle: Duration) {
  private val log = org.slf4j.LoggerFactory.getLogger(this.getClass)
  type K = String

  val takeSpinCountDelay: Int = ConfigManager.takeSpinCountDelay

  val defaultCachingSettings: CachingSettings = CachingSettings(system)

  val lfuCacheSettings: LfuCacheSettings = //Minimum use of exclusion algorithm cache
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(size) //Starting unit
      .withMaxCapacity(size) //Maximum Unit
      .withTimeToLive(newTimeToLive) //Maximum retention time
      .withTimeToIdle(newTimeToIdle) //Maximum Unused Time

  val cachingSettings: CachingSettings =
    defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

  val lfuCache: Cache[K, V] = LfuCache[K, V](cachingSettings)

  log.info("AkkaCache config. " +
    s"withInitialCapacity=$size, " +
    s"withMaxCapacity=$size, " +
    s"withTimeToLive=$newTimeToLive, " +
    s"withTimeToIdle=$newTimeToIdle, " +
    s"takeSpinCountDelay=$takeSpinCountDelay"
  )

  def take(key: K, atMost: Long): Option[Future[V]] = {
    var value = lfuCache.get(key)
    var count = 0
    while (value.isEmpty && (atMost / takeSpinCountDelay) > count) {
      Thread.sleep(takeSpinCountDelay)
      value = lfuCache.get(key)
      count = count + 1
    }
    value
  }

  def put(key: K, value: V): Future[V] = {
    lfuCache.put(key, Future.successful(value))
  }

}
