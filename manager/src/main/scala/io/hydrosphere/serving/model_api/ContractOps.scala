package io.hydrosphere.serving.model_api

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_field.ModelField.InfoOrDict.{Dict, Empty, Info}
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import io.hydrosphere.serving.tensorflow.types.DataType._
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue}

object ContractOps {

  case class SignatureDescription(signatureName: String, inputs: List[FieldDescription], outputs: List[FieldDescription])

  case class FieldDescription(fieldName: String, dataType: DataType, shape: Option[List[Long]])

  object Implicits {

    implicit class ModelContractPumped(modelContract: ModelContract) {
      def flatten: List[SignatureDescription] = {
        ModelContractOps.flatten(modelContract)
      }
    }

    implicit class ModelSignaturePumped(modelSignature: ModelSignature) {
      def +++(other: ModelSignature): ModelSignature = {
        ModelSignatureOps.merge(modelSignature, other)
      }
    }

    implicit class TensorShapeProtoPumped(tensorShapeProto: TensorShapeProto) {
      def toDimList: List[Long] = {
        tensorShapeProto.dim.map(_.size).toList
      }
    }

    implicit class TensorProtoPumped(tensorProto: TensorProto) {
      def jsonify: JsValue = {
        TensorProtoOps.jsonify(tensorProto)
      }
    }

  }

  object ModelContractOps {
    def flatten(modelContract: ModelContract): List[SignatureDescription] = {
      modelContract.signatures.map(ModelSignatureOps.flatten).toList
    }
  }

  object ModelSignatureOps {
    def merge(signature1: ModelSignature, signature2: ModelSignature): ModelSignature = {
      val mergedIns = ModelFieldOps.merge(signature1.inputs, signature2.inputs)
      val mergedOuts = ModelFieldOps.merge(signature1.outputs, signature2.outputs)
      ModelSignature(
        s"${signature1.signatureName}&${signature2.signatureName}",
        mergedIns,
        mergedOuts
      )
    }

    def flatten(modelSignature: ModelSignature): SignatureDescription = {
      val inputs = ModelFieldOps.flatten(modelSignature.inputs)
      val outputs = ModelFieldOps.flatten(modelSignature.outputs)
      SignatureDescription(modelSignature.signatureName, inputs, outputs)
    }
  }

  object ModelFieldOps {
    def merge(inputs: Seq[ModelField], inputs1: Seq[ModelField]): Seq[ModelField] = {
      inputs.zip(inputs1).flatMap {
        case (in1, in2) =>
          if (in1.fieldName == in2.fieldName) {
            val merged = merge(in1, in2).getOrElse(throw new IllegalArgumentException(s"$in1 and $in2 aren't mergeable"))
            List(merged)
          } else {
            List(in1, in2)
          }
      }
    }

    def merge(first: ModelField, second: ModelField): Option[ModelField] = {
      if (first == second) {
        Some(first)
      } else if (first.fieldName == second.fieldName) {
        val fieldContents = first.infoOrDict -> second.infoOrDict match {
          case (Dict(fDict), Dict(sDict)) =>
            mergeDicts(fDict, sDict).map(ModelField.InfoOrDict.Dict.apply)
          case (Info(fInfo), Info(sInfo)) =>
            TensorInfoOps.merge(fInfo, sInfo).map(ModelField.InfoOrDict.Info.apply)
          case _ => None
        }
        fieldContents.map(ModelField(first.fieldName, _))
      } else {
        None
      }
    }

    def mergeDicts(first: ModelField.Dict, second: ModelField.Dict): Option[ModelField.Dict] = {
      val fields = second.data.map {
        case (name, field) =>
          val emitterField = first.data.get(name)
          name -> emitterField.flatMap(merge(_, field))
      }
      if (fields.forall(_._2.isDefined)) {
        val exactFields = fields.map {
          case (name, field) => name -> field.get
        }
        Some(ModelField.Dict(exactFields))
      } else {
        None
      }
    }

    def flatten(fields: Seq[ModelField], rootName: String = ""): List[FieldDescription] = {
      fields.flatMap(flatten(rootName, _)).toList
    }

    def flatten(rootName: String, field: ModelField): List[FieldDescription] = {
      val name = s"$rootName/${field.fieldName}"
      field.infoOrDict match {
        case Empty => List.empty
        case Dict(value) =>
          value.data.flatMap {
            case (key, subfield) =>
              flatten(s"$name/$key", subfield)
          }.toList
        case Info(value) =>
          List(TensorInfoOps.flatten(name, value))
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
            Some(TensorInfo(first.name, first.dtype, shape))
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

    case class ColumnShaper(tensorShapeProto: Option[TensorShapeProto]) {
      def apply(data: Seq[JsValue]): JsValue = {
        tensorShapeProto match {
          case Some(shape) =>
            val dims = shape.dim.map(_.size).reverseIterator
            shapeGrouped(JsArray(data.toVector), dims).elements.head
          case None => data.head // as-is because None shape is a scalar
        }
      }

      def shapeGrouped(data: JsArray, shapeIter: Iterator[Long]): JsArray = {
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

  object TensorShapeProtoOps {
    def merge(first: TensorShapeProto, second: TensorShapeProto): Option[TensorShapeProto] = {
      if (first.dim.length != second.dim.length) {
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

}
