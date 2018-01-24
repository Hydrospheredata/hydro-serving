package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.manager.model.api.description.FieldDescription
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo

trait TensorInfoOps {
  implicit class TensorInfoPumped(tensorInfo: TensorInfo) {
    def flatten(rootName: String = ""): FieldDescription = {
      TensorInfoOps.flatten(rootName, tensorInfo)
    }
  }
}

object TensorInfoOps {
  def merge(first: TensorInfo, second: TensorInfo): Option[TensorInfo] = {
    if (first.dtype != second.dtype) {
      None
    } else {
      first.tensorShape -> second.tensorShape match {
        case (em, re) if em == re => Some(first)
        case (Some(em), Some(re)) if re.unknownRank == em.unknownRank && re.unknownRank => Some(first)
        case (Some(em), Some(re)) =>
          val shape = TensorShapeProtoOps.merge(em, re)
          Some(TensorInfo(first.dtype, shape))
        case _ => None
      }
    }
  }

  def flatten(rootName: String, tensor: TensorInfo): FieldDescription = {
    FieldDescription(
      rootName,
      tensor.dtype,
      TensorShapeProtoOps.shapeToList(tensor.tensorShape)
    )
  }
}