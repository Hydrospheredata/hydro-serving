package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.tensorflow.api.predict.PredictResponse
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType._
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue}

import scala.annotation.tailrec

trait TensorProtoOps {

  implicit class TensorProtoPumped(tensorProto: TensorProto) {
    def jsonify: JsValue = {
      TensorProtoOps.jsonify(tensorProto)
    }
  }

}

object TensorProtoOps {

  def jsonify(tensorProto: TensorProto): JsValue = {
    if (tensorProto.dtype == DT_MAP) {
      JsObject(
        tensorProto.mapVal.map {
          case (name, subTensor) =>
            name -> jsonify(subTensor)
        }
      )
    } else {
      val shaper = ColumnShaper(tensorProto.tensorShape)
      val data = tensorProto.dtype match {
        case DT_FLOAT => tensorProto.floatVal.map(JsNumber.apply(_))
        case DT_DOUBLE => tensorProto.doubleVal.map(JsNumber.apply)
        case DT_INT8 | DT_INT16 | DT_INT32 => tensorProto.intVal.map(JsNumber.apply)
        case DT_UINT8 | DT_UINT16 | DT_UINT32 => tensorProto.uint32Val.map(JsNumber.apply)
        case DT_INT64 => tensorProto.int64Val.map(JsNumber.apply)
        case DT_UINT64 => tensorProto.uint64Val.map(JsNumber.apply)

        case DT_QINT8 | DT_QINT16 | DT_QINT32 => tensorProto.intVal.map(JsNumber.apply)
        case DT_QUINT8 | DT_QUINT16 => tensorProto.uint32Val.map(JsNumber.apply)
        case DT_COMPLEX64 => tensorProto.scomplexVal.map(JsNumber.apply(_))
        case DT_COMPLEX128 => tensorProto.dcomplexVal.map(JsNumber.apply)

        case DT_STRING => tensorProto.stringVal.map(_.toStringUtf8()).map(JsString.apply)
        case DT_BOOL => tensorProto.boolVal.map(JsBoolean.apply)
        case x => throw new IllegalArgumentException(s"Can't jsonify unsupported TensorProto dtype $x")
      }
      shaper(data)
    }
  }

  def jsonify(tensors: Map[String, TensorProto]): JsObject = {
    JsObject(
      tensors.map {
        case (name, tensor) =>
          name -> jsonify(tensor)
      }
    )
  }

  def jsonify(predictResponse: PredictResponse): JsObject = {
    jsonify(predictResponse.outputs)
  }

  case class ColumnShaper(tensorShapeProto: Option[TensorShapeProto]) {
    def apply(data: Seq[JsValue]): JsValue = {
      tensorShapeProto match {
        case Some(shape) =>
          val dims = shape.dim.map(_.size).reverseIterator
          shapeGrouped(JsArray(data.toVector), dims).elements.head
        case None => data.head // as-is because None shape is a scalar
      }
    }

    @tailrec
    final def shapeGrouped(data: JsArray, shapeIter: Iterator[Long]): JsArray = {
      if (shapeIter.nonEmpty) {
        val dimShape = shapeIter.next()
        if (dimShape == -1) {
          shapeGrouped(data, shapeIter)
        } else {
          shapeGrouped(JsArray(
            data
              .elements
              .grouped(dimShape.toInt)
              .map(JsArray.apply)
              .toVector),
            shapeIter)
        }
      } else {
        data
      }
    }
  }
}