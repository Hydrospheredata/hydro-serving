package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.TensorShape
import spray.json.{JsArray, JsObject, JsValue}

import scala.annotation.tailrec

case class ColumnShaper(tensorShapeProto: TensorShape) {
  def apply(data: Seq[JsValue]): JsValue = {
    tensorShapeProto.dims match {
      case Some(shape) =>
        val dims = shape.reverseIterator
        shapeGrouped(JsArray(data.toVector), dims)
      case None => data.headOption.getOrElse(JsObject.empty)
    }
  }

  @tailrec
  final def shapeGrouped(data: JsArray, shapeIter: Iterator[Long]): JsArray = {
    if (shapeIter.nonEmpty) {
      val dimShape = shapeIter.next()
      val dataDim = data.elements.length
      val maybeReshaped = dimShape match {
        case -1 => data
        case `dataDim` => data
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
