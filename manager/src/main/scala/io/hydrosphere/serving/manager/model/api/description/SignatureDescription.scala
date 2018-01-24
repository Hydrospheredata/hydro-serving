package io.hydrosphere.serving.manager.model.api.description

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.ContractBuilders
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo

import scala.collection.mutable

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
            ContractBuilders.createTensorInfo(field.dataType, field.shape)
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
            ContractBuilders.createTensorInfo(field.dataType, field.shape)
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
