package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_field.ModelField.InfoOrSubfields.{Empty, Info, Subfields}
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto

object SignatureChecker {

  def areCompatible(first: Seq[TensorShapeProto.Dim], second: Seq[TensorShapeProto.Dim]): Boolean = {
    if (first.lengthCompare(second.length) != 0) {
      false
    } else {
      first.zip(second).forall{
        case (em, re) =>
          if (re.size == -1) {
            true
          } else {
            em.size == re.size
          }
      }
    }
  }

  def areSequentiallyCompatible(emitter: TensorInfo, receiver: TensorInfo): Boolean = {
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

  def areSequentiallyCompatible(emitter: ModelField.ComplexField, receiver: ModelField.ComplexField): Boolean = {
    receiver.data.forall { field =>
        val emitterField = emitter.data.find(_.fieldName == field.fieldName)
        emitterField.exists(areSequentiallyCompatible(_, field))
    }
  }

  def areSequentiallyCompatible(emitter: ModelField, receiver: ModelField): Boolean = {
    if (emitter == receiver) {
      true
    } else if (emitter.fieldName == receiver.fieldName) {
      emitter.infoOrSubfields match {
        case Empty => receiver.infoOrSubfields.isEmpty
        case Subfields(fields) =>
          receiver.infoOrSubfields.subfields.exists(areSequentiallyCompatible(fields, _))
        case Info(tensor) =>
          receiver.infoOrSubfields.info.exists(areSequentiallyCompatible(tensor, _))
      }
    } else {
      false
    }
  }

  def areSequentiallyCompatible(emitter: ModelSignature, receiver: ModelSignature): Boolean = {
    if (receiver.inputs.isEmpty) {
      false
    } else {
      val outputMap = emitter.outputs.map(i => i.fieldName -> i).toMap
      receiver.inputs.forall { input =>
        outputMap
          .get(input.fieldName)
          .exists(in => areSequentiallyCompatible(in, input))
      }
    }
  }
}