package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.TensorShape.{AnyDims, Dims}
import spray.json.{JsArray, JsObject, JsValue}

import scala.annotation.tailrec

case class ColumnShaper(tensorShape: TensorShape) {
  def apply(data: Seq[JsValue]): JsValue = {
    tensorShape match {
      case AnyDims() => data.headOption.getOrElse(JsObject.empty)
      case Dims(dims, _) if dims.isEmpty => data.headOption.getOrElse(JsObject.empty)
      case Dims(dims, _) => shapeRaw(data, dims)
    }
  }

  final def shapeRaw(data: Seq[JsValue], dims: Seq[Long]): JsArray = {
    shapeGrouped(JsArray(data.toVector), dims.reverseIterator)
  }

  @tailrec
  final def shapeGrouped(data: JsArray, shapeIter: Iterator[Long]): JsArray = {
    if (shapeIter.nonEmpty) {
      val dimShape = shapeIter.next()
      val dataDim = data.elements.length
      val maybeReshaped = dimShape match {
        case -1 => data
        case `dataDim` => data
        case 1 => JsArray(data)
        case toReshape => JsArray(
          data
            .elements
            .grouped(toReshape.toInt)
            .map(JsArray.apply)
            .toVector
        )
      }
      shapeGrouped(maybeReshaped, shapeIter)
    } else {
      data
    }
  }
}

//
//1, 4
//
//1,2,3,4
//
//[1, 2, 3, 4]

