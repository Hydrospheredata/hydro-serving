package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto

trait TensorShapeProtoOps {

  implicit class TensorShapeProtoPumped(tensorShapeProto: TensorShapeProto) {
    def toDimList: List[Long] = {
      tensorShapeProto.dim.map(_.size).toList
    }
  }

}

object TensorShapeProtoOps {
  def merge(first: TensorShapeProto, second: TensorShapeProto): Option[TensorShapeProto] = {
    if (first.dim.lengthCompare(second.dim.length) != 0) {
      None
    } else {
      val dims = first.dim.zip(second.dim).map {
        case (fDim, sDim) if fDim.size == sDim.size => Some(fDim)
        case (fDim, sDim) if fDim.size == -1 => Some(sDim)
        case (fDim, sDim) if sDim.size == -1 => Some(fDim)
        case _ => None
      }
      if (dims.forall(_.isDefined)) {
        Some(TensorShapeProto(dims.map(_.get)))
      } else {
        None
      }
    }
  }

  def shapeToList(tensorShapeProto: Option[TensorShapeProto]): Option[List[Long]] = {
    tensorShapeProto.map { shape =>
      shape.dim.map { dim =>
        dim.size
      }.toList
    }
  }
}