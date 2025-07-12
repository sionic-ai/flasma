package serving.model

import org.jetbrains.bio.npy.NpyFile
import org.json4s.DefaultFormats
import org.tensorflow.{Graph, Tensor}
import org.tensorflow.ndarray.Shape
import org.tensorflow.ndarray.buffer.DataBuffers
import org.tensorflow.op.Ops
import org.tensorflow.op.core.{Constant, Placeholder}
import org.tensorflow.op.nn.TopK
import org.tensorflow.types.TFloat32
import serving.config.ConfigManager
import serving.tensor.TensorFlowProvider
import serving.tensor.InputTensor

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.file.Paths

final case class Vec(vec: Seq[(String, Array[Float])])

object CosineSimilarity {
  private val log = org.slf4j.LoggerFactory.getLogger(this.getClass)
  implicit val formats: DefaultFormats = DefaultFormats

  private val wLength: Int = ConfigManager.sample
  private val dim: Int = ConfigManager.dim
  private val topK: Int = ConfigManager.topK

  log.info(s"CosineSimilarity Initialize start")

  private val dataVectorNumpyArray: Array[Float] = {
    val npyFilePath = ConfigManager.npyFile
    log.info(s"Npy file load = ${npyFilePath}")
    NpyFile.read(Paths.get(npyFilePath), Int.MaxValue).asFloatArray()
  }

  private val modelProvider: TensorFlowProvider = new TensorFlowProvider(model(k = topK))

  def model(k: Int): Graph = {

    //val wArray: Array[Float] = dataVectorNumpyArray
    val wArray: Array[Float] = Array.fill(dim * wLength)(0.0f)

    assert(wArray.length == (dim * wLength), f"${wArray.length} != ${dim} * ${wLength} npy file does not match size and dimension and sample length. ")

    val graph = new Graph()
    val tf = Ops.create(graph)

    val wTensor = TFloat32.tensorOf(Shape.of(1, dim, wLength))

    val chunkSize = 100000
    var offset = 0

    while (offset < wArray.length) {
      val length = Math.min(chunkSize, wArray.length - offset)
      println(length)
      // 100만 개씩 데이터를 복사하여 바이트버퍼로 변환
      val byteBuffer = ByteBuffer.allocateDirect(length * 4).order(ByteOrder.nativeOrder())

      for (i <- 0 until length) {
        byteBuffer.putFloat(wArray(offset + i))
      }

      byteBuffer.flip()

      val byteArray = new Array[Byte](length * 4)
      byteBuffer.get(byteArray)

      // wTensor에 기록
      wTensor.asRawTensor().data().write(byteArray)

      offset += length
    }


    val vTensor = tf.withName("input").placeholder(classOf[TFloat32],
      Placeholder.shape(Shape.of(-1, dim, 1)))
    val mul = tf.math.mul(vTensor, tf.constant(wTensor))
    val cosineSimilarity = tf.reduceSum(mul, tf.array(1))
    val nnTopK = tf.withName("output").nn.topK(cosineSimilarity, tf.constant(k), Array.empty[TopK.Options])

    graph
  }

  def run(v: Array[Array[Float]], dim: Int = dim, k: Int): Array[Array[(Int, Float)]] = {
    val startTime = System.currentTimeMillis()

    val vLength: Int = v.length
    val vTensor: TFloat32 = Tensor.of(classOf[TFloat32], Shape.of(vLength, dim, 1))
    val vArray: Array[Float] = v.flatten

    DataBuffers.ofFloats(vLength * dim).write(vArray)
      .copyTo(vTensor.asRawTensor().data().asFloats(), vLength * dim)

    val inputTensors = Seq(
      InputTensor("input", vTensor)
    )

    val outputTensors: Seq[(String, Int)] = Seq(
      ("output", 0),
      ("output", 1)
    )

    val resultTensors = modelProvider.run(inputTensors, outputTensors)

    val scores = resultTensors.get(outputTensors.head)
      .map(x => {
        val tensor = x
        val size = tensor.shape().asArray().product.toInt
        val array = Array.ofDim[Float](size)
        tensor.asRawTensor().data().asFloats().read(array)
        array.grouped(k)
      }).head

    val index = resultTensors.get(outputTensors.last)
      .map(x => {
        val tensor = x
        val size = tensor.shape().asArray().product.toInt
        val array = Array.ofDim[Int](size)
        tensor.asRawTensor().data().asInts().read(array)
        array.grouped(k)
      }).head

    log.debug(s"cosineSimilarity elapsedTime=${System.currentTimeMillis() - startTime}ms")
    (index zip scores).map(x => x._1 zip x._2).map(y => y).toArray
  }

  def apply(v: Array[Array[Float]]): Array[Array[(Int, Float)]] = {
    run(v, k = topK)
  }

  def main(args: Array[String]): Unit = {
    val a = run(Array(Array.fill(100)(0.0f)),k = 5)
    a.foreach(x=>x.foreach(println))
}
}
