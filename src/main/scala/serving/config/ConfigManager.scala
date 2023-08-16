package serving.config

object ConfigManager {
  private val log = org.slf4j.LoggerFactory.getLogger(this.getClass)
  val port: Int =  System.getProperty("port","8080").toInt
  val timeout: Int =  System.getProperty("timeout","10000").toInt
  val takeSpinCountDelay : Int =  System.getProperty("takeSpinCountDelay","5").toInt

  val topK: Int = System.getProperty("topK","10").toInt
  val dim: Int = System.getProperty("dim","100").toInt
  val sample: Int =  System.getProperty("sample","10000").toInt
  val batch: Int =  System.getProperty("batch","16").toInt
  val npyFile: String = System.getProperty("npyFile", "./model/10000-100.npy")

  log.info("Load config. " +
    s"port=$port, " +
    s"batch=$batch, " +
    s"timeout=$timeout, " +
    s"takeSpinCountDelay=$takeSpinCountDelay, " +
    s"topK=$topK, " +
    s"dim=$dim, " +
    s"sample=$sample, " +
    s"batch=$batch, " +
    s"npyFile=$npyFile"
  )

}
