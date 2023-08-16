package serving.tensor

import org.tensorflow.{Graph, Session, Tensor}

import scala.collection.JavaConverters._
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

case class InputTensor(inputLayerNames: String, tensor: Tensor)

class TensorFlowProvider(graph: Graph) extends AutoCloseable {

  private val session: Session = new Session(graph)

  def run(inputTensors: Seq[InputTensor],
            outputs: Seq[(String, Int)]):  Map[(String, Int), Tensor] = {
    val runner = session.runner()

    // feed
    inputTensors foreach { i =>
      runner.feed(i.inputLayerNames, i.tensor)
    }

    // fetch
    outputs foreach { op =>
      runner.fetch(op._1, op._2)
    }

    // run. This takes most of the time
    val resultTensors: Map[(String, Int), Tensor] = (runner.run().asScala zip outputs)
      .map(x => (x._2._1,  x._2._2) -> x._1).toMap

    //release
    inputTensors.foreach(_.tensor.close())
    resultTensors
  }

  def modelInfo(): String = {
    val nodeList = graph.toGraphDef.getNodeList.map(x => x.getName).mkString(", ")
    Map("nodeList" -> nodeList).mkString
  }

  def nodeNameInfo(name: String): String = {
    val nodeList = graph.toGraphDef.getNodeList.flatMap(x => Map(x.getName -> x.toString)).toMap
    Map("node" -> nodeList.getOrElse(name, "")).mkString
  }

  override def close(): Unit = {
    session.close()
    graph.close()
  }
}

