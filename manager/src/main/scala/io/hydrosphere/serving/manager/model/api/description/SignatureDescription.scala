package io.hydrosphere.serving.manager.model.api.description

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.TensorShape.{AnyDims, Dims}
import io.hydrosphere.serving.tensorflow.types.DataType

import scala.collection.mutable

case class SignatureDescription(
  signatureName: String,
  inputs: Seq[FieldDescription],
  outputs: Seq[FieldDescription]
) {
  def toSignature: ModelSignature = SignatureDescription.toSignature(this)
}

object SignatureDescription {

  class Converter() {

    sealed trait ANode {
      def name: String

      def shape: TensorShape

      def toTypeOrSubfields: ModelField.TypeOrSubfields
    }

    case class FTensor(name: String, shape: TensorShape, dtype: DataType) extends ANode {
      override def toTypeOrSubfields = {
        ModelField.TypeOrSubfields.Dtype(dtype)
      }
    }

    case class FMap(name: String = "", shape: TensorShape = AnyDims(), data: mutable.ListBuffer[ANode] = mutable.ListBuffer.empty)
      extends ANode {
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

      override def toTypeOrSubfields = {
        ModelField.TypeOrSubfields.Subfields(
          ModelField.Subfield(
            data.map { node =>
              ModelField(node.name, node.shape.toProto, node.toTypeOrSubfields)
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
      val shape = field.shape match {
        case Some(x) => Dims(x)
        case None => AnyDims()
      }
      field.fieldName.split("/").drop(1).toList match {
        case tensorName :: Nil =>
          tree += FTensor(
            tensorName,
            shape,
            field.dataType
          )
        case root :: segments =>
          var last = tree.getOrUpdate(root, FMap(root)).asInstanceOf[FMap]

          segments.init.foreach { segment =>
            val segField = last.getOrUpdate(segment, FMap(segment)).asInstanceOf[FMap]
            last += segField
            last = segField
          }

          val lastName = segments.last
          last += FTensor(
            lastName,
            shape,
            field.dataType
          )
        case Nil =>
          throw new IllegalArgumentException(
            s"Field name '${field.fieldName}' in flattened contract is incorrect."
          )
      }
    }

    def result: Seq[ModelField] = {
      tree.data.map { node =>
        ModelField(node.name, node.shape.toProto, node.toTypeOrSubfields)
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
