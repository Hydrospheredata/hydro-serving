package io.hydrosphere.serving.model_api

import hydroserving.contract.model_field.ModelField
import hydroserving.contract.model_field.ModelField.InfoOrDict.{Dict, Info}
import hydroserving.contract.model_signature.ModelSignature
import hydroserving.tensorflow.tensor_info.TensorInfo
import hydroserving.tensorflow.tensor_shape.TensorShapeProto

object SignatureOps {

  object Implicits {

    implicit class MergeableModelSignature(modelSignature: ModelSignature) {
      def +++(other: ModelSignature): ModelSignature = {
        SignatureOps.merge(modelSignature, other)
      }
    }

  }

  def mergeDicts(first: ModelField.Dict, second: ModelField.Dict): Option[ModelField.Dict] = {
    val fields = second.data.map {
      case (name, field) =>
        val emitterField = first.data.get(name)
        name -> emitterField.flatMap(mergeModelField(_, field))
    }
    if (fields.forall(_._2.isDefined)) {
      val exactFields = fields.map{
        case (name, field) => name -> field.get
      }
      Some(ModelField.Dict(exactFields))
    } else {
      None
    }
  }

  def mergeDims(first: Seq[TensorShapeProto.Dim], second: Seq[TensorShapeProto.Dim]): Option[Seq[TensorShapeProto.Dim]] = {
    if (first.length != second.length) {
      None
    } else {
      val dims = first.zip(second).map {
        case (fDim, sDim) if fDim.size == sDim.size => Some(fDim)
        case (fDim, sDim) if fDim.size == -1 => Some(sDim)
        case (fDim, sDim) if sDim.size == -1 => Some(fDim)
        case _ => None
      }
      if (dims.forall(_.isDefined)) {
        Some(dims.map(_.get))
      } else {
        None
      }
    }
  }

  def mergeTensorInfo(first: TensorInfo, second: TensorInfo): Option[TensorInfo] = {
    if (first.dtype != second.dtype) {
      None
    } else {
      first.tensorShape -> second.tensorShape match {
        case (em, re) if em == re => Some(first)
        case (Some(em), Some(re)) if re.unknownRank == em.unknownRank && re.unknownRank => Some(first)
        case (Some(em), Some(re)) =>
          mergeDims(em.dim, re.dim).map{ dims =>
            TensorInfo(first.name, first.dtype, Some(TensorShapeProto(dims)))
          }
        case _ => None
      }
    }
  }

  def mergeModelField(first: ModelField, second: ModelField): Option[ModelField] = {
    if (first == second) {
      Some(first)
    } else if (first.fieldName == second.fieldName) {
      val fieldContents = first.infoOrDict -> second.infoOrDict match {
        case (Dict(fDict), Dict(sDict)) =>
          mergeDicts(fDict, sDict).map(ModelField.InfoOrDict.Dict.apply)
        case (Info(fInfo), Info(sInfo)) =>
          mergeTensorInfo(fInfo, sInfo).map(ModelField.InfoOrDict.Info.apply)
        case _ => None
      }
      fieldContents.map(ModelField(first.fieldName, _))
    } else {
      None
    }
  }


  def merge(inputs: Seq[ModelField], inputs1: Seq[ModelField]): Seq[ModelField] = {
    inputs.zip(inputs1).flatMap {
      case (in1, in2) =>
        if (in1.fieldName == in2.fieldName) {
          val merged = mergeModelField(in1, in2).getOrElse(throw new IllegalArgumentException(s"$in1 and $in2 aren't mergeable"))
          List(merged)
        } else {
          List(in1, in2)
        }
    }
  }

  def merge(signature1: ModelSignature, signature2: ModelSignature): ModelSignature = {
    val mergedIns = merge(signature1.inputs, signature2.inputs)
    val mergedOuts = merge(signature1.outputs, signature2.outputs)
    ModelSignature(
      s"${signature1.signatureName}&${signature2.signatureName}",
      mergedIns,
      mergedOuts
    )
  }
}
