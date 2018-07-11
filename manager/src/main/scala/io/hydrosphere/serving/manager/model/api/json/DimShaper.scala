package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.TensorShape.{AnyDims, Dims}
import spray.json.{JsArray, JsObject, JsValue}

sealed trait ColumnShaper {
  def shape(data: Seq[JsValue]): JsValue
}

case class AnyShaper() extends ColumnShaper {
  override def shape(data: Seq[JsValue]): JsValue = {
    data.headOption.getOrElse(JsObject.empty)
  }
}

case class ScalarShaper() extends ColumnShaper {
  override def shape(data: Seq[JsValue]): JsValue = {
    data.headOption.getOrElse(JsObject.empty)
  }
}

case class DimShaper(dims: Seq[Long]) extends ColumnShaper {
  val strides: Seq[Long] = {
    val res = Array.fill(dims.length)(1L)
    val stLen = dims.length - 1
    for (i <- 0.until(stLen).reverse) {
      res(i) = res(i + 1) * dims(i + 1)
    }
    res.toSeq
  }

  def shape(data: Seq[JsValue]): JsValue = {
    def shapeGrouped(dataId: Int, shapeId: Int): JsValue = {
      if (shapeId >= dims.length) {
        data(dataId)
      } else {
        val n = dims(shapeId).toInt
        val stride = strides(shapeId).toInt
        var mDataId = dataId
        val res = new Array[JsValue](n)

        for (i <- 0.until(n)) {
          val item = shapeGrouped(mDataId, shapeId + 1)
          res(i) = item
          mDataId += stride
        }
        JsArray(res.toVector)
      }
    } // def shapeGrouped

    shapeGrouped(0, 0)
  }
}

object ColumnShaper {
  def apply(tensorShape: TensorShape): ColumnShaper = {
    tensorShape match {
      case AnyDims() => AnyShaper()
      case Dims(dims, _) if dims.isEmpty => ScalarShaper()
      case Dims(dims, _) => DimShaper(dims)
    }
  }
}