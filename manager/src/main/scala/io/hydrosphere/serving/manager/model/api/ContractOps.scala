package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_field.ModelField.InfoOrSubfields.{Empty, Info, Subfields}
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import io.hydrosphere.serving.tensorflow.types.DataType._
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue}

import scala.annotation.tailrec
import scala.collection.mutable


object ContractOps {

  case class ContractDescription(
    signatures: List[SignatureDescription]
  ) {
    def toContract: ModelContract = ContractDescription.toContract(this)
  }

  object ContractDescription {
    def toContract(contractDescription: ContractDescription): ModelContract = {
      ModelContract(
        modelName = "",
        signatures = contractDescription.signatures.map(SignatureDescription.toSignature)
      )
    }
  }

  case class SignatureDescription(
    signatureName: String,
    inputs: List[FieldDescription],
    outputs: List[FieldDescription]
  ) {
    def toSignature: ModelSignature = SignatureDescription.toSignature(this)
  }

  object SignatureDescription {
    class Converter() {

      sealed trait ANode {
        def name: String

        def toInfoOrDict: ModelField.InfoOrSubfields
      }

      case class FTensor(name: String, tensorInfo: TensorInfo) extends ANode {
        override def toInfoOrDict: ModelField.InfoOrSubfields = {
          ModelField.InfoOrSubfields.Info(tensorInfo)
        }
      }

      case class FMap(name: String = "", data: mutable.ListBuffer[ANode] = mutable.ListBuffer.empty) extends ANode {
        def getOrUpdate(segment: String, map: FMap): ANode = {
          data.find(_.name == segment) match {
            case Some(x) =>
              x
            case None =>
              data += map
              map
          }
        }

        def +=(node: ANode): data.type = data += node

        override def toInfoOrDict: ModelField.InfoOrSubfields = {
          ModelField.InfoOrSubfields.Subfields(
            ModelField.ComplexField(
              data.map { node =>
                ModelField(node.name, node.toInfoOrDict)
              }
            )
          )
        }
      }

      private val tree = FMap()

      /**
        * Mutates tree to represent contract field in tree-like structure.
        * Inner state is contained in `tree` variable.
        *
        * @param field flattened field description
        */
      def acceptField(field: FieldDescription): Unit = {
        field.fieldName.split("/").drop(1).toList match {
          case (tensorName :: Nil) =>
            tree += FTensor(
              tensorName,
              ModelContractBuilders.createTensorInfo(field.dataType, field.shape)
            )
          case (root :: segments) =>
            var last = tree.getOrUpdate(root, FMap(root)).asInstanceOf[FMap]

            segments.init.foreach { segment =>
              val segField = last.getOrUpdate(segment, FMap(segment)).asInstanceOf[FMap]
              last += segField
              last = segField
            }

            val lastName = segments.last
            last += FTensor(
              lastName,
              ModelContractBuilders.createTensorInfo(field.dataType, field.shape)
            )
          case Nil => throw new IllegalArgumentException(s"Field name '${field.fieldName}' in flattened contract is incorrect.")
        }
      }

      def result: Seq[ModelField] = {
        tree.data.map { node =>
          ModelField(node.name, node.toInfoOrDict)
        }
      }
    }

    def toSignature(signatureDescription: SignatureDescription): ModelSignature = {
      ModelSignature(
        signatureName = signatureDescription.signatureName,
        inputs = SignatureDescription.toFields(signatureDescription.inputs),
        outputs = SignatureDescription.toFields(signatureDescription.outputs)
      )
    }

    def toFields(fields: Seq[FieldDescription]): Seq[ModelField] = {
      val converter = new Converter()
      fields.foreach(converter.acceptField)
      converter.result
    }
  }

  case class FieldDescription(
    fieldName: String,
    dataType: DataType,
    shape: Option[List[Long]]
  )

  object Implicits {

    implicit class ModelContractPumped(modelContract: ModelContract) {
      def flatten: ContractDescription = {
        ContractDescription(
          ModelContractOps.flatten(modelContract)
        )
      }
    }

    implicit class ModelSignaturePumped(modelSignature: ModelSignature) {
      def +++(other: ModelSignature): ModelSignature = {
        ModelSignatureOps.merge(modelSignature, other)
      }
    }

    implicit class ModelFieldPumped(modelField: ModelField) {
      def insert(name: String, fieldInfo: ModelField): Option[ModelField] = {
        modelField.infoOrSubfields match {
          case Subfields(fields) =>
            fields.data.find(_.fieldName == name) match {
              case Some(_) =>
                None
              case None =>
                val newData = fields.data :+ fieldInfo
                Some(ModelContractBuilders.complexField(modelField.fieldName, newData))
            }
          case _ => None
        }
      }

      def child(name: String): Option[ModelField] = {
        modelField.infoOrSubfields match {
          case Subfields(value) =>
            value.data.find(_.fieldName == name)
          case _ => None
        }
      }

      def search(name: String): Option[ModelField] = {
        modelField.infoOrSubfields match {
          case Subfields(value) =>
            value.data.find(_.fieldName == name).orElse {
              value.data.flatMap(_.search(name)).headOption
            }
          case _ => None
        }
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
        val fieldContents = first.infoOrSubfields -> second.infoOrSubfields match {
          case (Subfields(fDict), Subfields(sDict)) =>
            mergeComplexFields(fDict, sDict).map(ModelField.InfoOrSubfields.Subfields.apply)
          case (Info(fInfo), Info(sInfo)) =>
            TensorInfoOps.merge(fInfo, sInfo).map(ModelField.InfoOrSubfields.Info.apply)
          case _ => None
        }
        fieldContents.map(ModelField(first.fieldName, _))
      } else {
        None
      }
    }

    def mergeComplexFields(first: ModelField.ComplexField, second: ModelField.ComplexField): Option[ModelField.ComplexField] = {
      val fields = second.data.map { field =>
          val emitterField = first.data.find(_.fieldName == field.fieldName)
          emitterField.flatMap(merge(_, field))
      }
      if (fields.forall(_.isDefined)) {
        val exactFields = fields.flatten
        Some(ModelField.ComplexField(exactFields))
      } else {
        None
      }
    }

    def flatten(fields: Seq[ModelField], rootName: String = ""): List[FieldDescription] = {
      fields.flatMap(flatten(rootName, _)).toList
    }

    def flatten(rootName: String, field: ModelField): Seq[FieldDescription] = {
      val name = s"$rootName/${field.fieldName}"
      field.infoOrSubfields match {
        case Empty => List.empty
        case Subfields(value) =>
          value.data.flatMap { subfield =>
              flatten(name, subfield)
          }
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

}
