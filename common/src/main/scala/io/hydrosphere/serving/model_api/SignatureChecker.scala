package io.hydrosphere.serving.model_api

import hydroserving.contract.model_field.ModelField
import hydroserving.contract.model_field.ModelField.InfoOrDict.{Dict, Empty, Info}
import hydroserving.contract.model_signature.ModelSignature
import hydroserving.tensorflow.tensor_info.TensorInfo
import hydroserving.tensorflow.tensor_shape.TensorShapeProto

object SignatureChecker {
  def areCompatible(emitter: Seq[TensorShapeProto.Dim], receiver: Seq[TensorShapeProto.Dim]): Boolean = {
    if (emitter.length != receiver.length) {
      false
    } else {
      emitter.zip(receiver).forall{
        case (em, re) =>
          if (re.size == -1) {
            true
          } else {
            em.size == re.size
          }
      }
    }
  }

  def areCompatible(emitter: TensorInfo, receiver: TensorInfo): Boolean = {
    if (emitter.dtype != receiver.dtype) {
      false
    } else {
      emitter.tensorShape -> receiver.tensorShape match {
        case (em, re) if em == re => true // two identical tensors
        case (em, re) if em.isDefined != re.isDefined => false // comparing scalar and dimensional tensor
        case (Some(_), Some(re)) if re.unknownRank => true // receiver has unknown rank - runtime check
        case (Some(em), Some(_)) if em.unknownRank => false
        case (Some(em), Some(re)) => areCompatible(em.dim, re.dim)
      }
    }
  }

  def areCompatible(emitter: ModelField.Dict, receiver: ModelField.Dict): Boolean = {
    receiver.data.forall {
      case (name, field) =>
        val emitterField = emitter.data.get(name)
        emitterField.exists(areCompatible(_, field))
    }
  }

  def areCompatible(emitter: ModelField, receiver: ModelField): Boolean = {
    if (emitter == receiver) {
      true
    } else if (emitter.fieldName == receiver.fieldName) {
      emitter.infoOrDict match {
        case Empty => receiver.infoOrDict.isInfo
        case Dict(dict) =>
          receiver.infoOrDict.dict.exists(areCompatible(dict, _))
        case Info(tensor) =>
          receiver.infoOrDict.info.exists(areCompatible(tensor, _))
      }
    } else {
      false
    }
  }

  def areCompatible(emitter: ModelSignature, receiver: ModelSignature): Boolean = {
    if (receiver.inputs.isEmpty) {
      false
    } else {
      val outputMap = emitter.outputs.map(i => i.fieldName -> i).toMap
      receiver.inputs.forall { input =>
        outputMap
          .get(input.fieldName)
          .exists(in => areCompatible(in, input))
      }
    }
  }
}
