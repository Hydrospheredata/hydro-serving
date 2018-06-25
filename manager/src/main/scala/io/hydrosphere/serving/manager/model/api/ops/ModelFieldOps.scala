package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_field.ModelField.TypeOrSubfields
import io.hydrosphere.serving.contract.model_field.ModelField.TypeOrSubfields.{Dtype, Empty, Subfields}
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.manager.model.api.description.FieldDescription
import io.hydrosphere.serving.manager.model.{HResult, Result}
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.TensorShape.{AnyDims, Dims}

trait ModelFieldOps {

  implicit class ModelFieldPumped(modelField: ModelField) {

    def flatten(rootName: String = ""): Seq[FieldDescription] = {
      ModelFieldOps.flatten(rootName, modelField)
    }

    def insert(name: String, fieldInfo: ModelField): Option[ModelField] = {
      modelField.typeOrSubfields match {
        case Subfields(fields) =>
          fields.data.find(_.name == name) match {
            case Some(_) =>
              None
            case None =>
              val newData = fields.data :+ fieldInfo
              Some(ContractBuilders.complexField(modelField.name, fieldInfo.shape, newData))
          }
        case _ => None
      }
    }

    def child(name: String): Option[ModelField] = {
      modelField.typeOrSubfields match {
        case Subfields(value) =>
          value.data.find(_.name == name)
        case _ => None
      }
    }

    def search(name: String): Option[ModelField] = {
      modelField.typeOrSubfields match {
        case Subfields(value) =>
          value.data.find(_.name == name).orElse {
            value.data.flatMap(_.search(name)).headOption
          }
        case _ => None
      }
    }

  }

  def mergeAll(inputs: Seq[ModelField], inputs1: Seq[ModelField]): Seq[ModelField] = {
    (inputs, inputs1) match {
      case (Nil, x) => x
      case (x, Nil) => x
      case (x, y) if x == y => x
      case _ => inputs.zip(inputs1).flatMap {
        case (in1, in2) =>
          if (in1.name == in2.name) {
            val merged = merge(in1, in2).getOrElse(throw new IllegalArgumentException(s"$in1 and $in2 aren't mergeable"))
            List(merged)
          } else {
            List(in1, in2)
          }
      }
    }
  }

  def merge(first: ModelField, second: ModelField): Option[ModelField] = {
    if (first == second) {
      Some(first)
    } else if (first.name == second.name) {
      val res = for {
        mergedShape <- mergeShapes(TensorShape(first.shape), TensorShape(second.shape)).right
        mergedType <- mergeTypeOrSubfields(first.typeOrSubfields, second.typeOrSubfields).right
      } yield {
        ModelField(first.name, mergedShape.toProto, mergedType)
      }
      res.right.toOption
    } else {
      None
    }
  }

  def mergeShapes(first: TensorShape, second: TensorShape): HResult[TensorShape] = {
    first -> second match {
      case (AnyDims(), AnyDims()) => Result.ok(first)
      case (AnyDims(), Dims(_,_)) => Result.ok(second) // todo maybe reconsider any with dim?
      case (Dims(_,_), AnyDims()) => Result.ok(first)  // todo maybe reconsider any with dim?

      case (Dims(fDims,_), Dims(sDims,_)) if fDims == sDims => Result.ok(first)
      case (Dims(fDims,_), Dims(sDims,_)) if fDims.length == sDims.length =>
        val dims = Result.traverse(fDims.zip(sDims)) {
          case (fDim, sDim) if fDim == sDim => Result.ok(fDim)
          case (fDim, sDim) if fDim == -1 => Result.ok(sDim)
          case (fDim, sDim) if sDim == -1 => Result.ok(fDim)
          case (fDim, sDim) => Result.clientError(s"Can't merge shape dims: $fDim and $sDim")
        }
        dims.right.map(Dims.apply(_))

      case (fd, sd) => Result.clientError(s"Unmergeable shapes: $fd and $sd")
    }
  }

  def mergeTypeOrSubfields(first: TypeOrSubfields, second: TypeOrSubfields): HResult[TypeOrSubfields] = {
    first -> second match {
      case (fDict: Subfields, sDict: Subfields) =>
        mergeSubfields(fDict, sDict)
      case (fInfo: Dtype, sInfo: Dtype) =>
        mergeTypes(fInfo, sInfo)
      case (x, y) => Result.clientError(s"Incompatible type combination: $x and $y")
    }
  }

  def mergeTypes(first: Dtype, second: Dtype): HResult[Dtype] = {
    first -> second match {
      case (Dtype(fInfo), Dtype(sInfo)) if fInfo == sInfo => Result.ok(Dtype(fInfo))
      case (Dtype(fInfo), Dtype(sInfo)) => Result.clientError(s"Incompatible types: $fInfo and $sInfo")
    }
  }

  def mergeSubfields(
    first: ModelField.TypeOrSubfields.Subfields,
    second: ModelField.TypeOrSubfields.Subfields
  ): HResult[ModelField.TypeOrSubfields.Subfields] = {
    val fields = second.value.data.map { field =>
      val emitterField = first.value.data.find(_.name == field.name)
      emitterField.flatMap(merge(_, field))
    }
    if (fields.forall(_.isDefined)) {
      val exactFields = fields.flatten
      Result.ok(TypeOrSubfields.Subfields(ModelField.Subfield(exactFields)))
    } else {
      Result.clientError(s"Subfields aren't mergeable: $first and $second")
    }
  }

  def flatten(fields: Seq[ModelField], rootName: String = ""): List[FieldDescription] = {
    fields.flatMap(flatten(rootName, _)).toList
  }

  def flatten(rootName: String, field: ModelField): Seq[FieldDescription] = {
    val name = s"$rootName/${field.name}"
    val simpleShape = TensorShape(field.shape) match {
      case AnyDims() => None
      case Dims(dims, _) => Some(dims)
    }
    field.typeOrSubfields match {
      case Empty => List.empty
      case Subfields(value) =>
        value.data.flatMap { subfield =>
          flatten(name, subfield)
        }
      case Dtype(value) =>
        List(
          FieldDescription(
            name,
            value,
            simpleShape
          )
        )
    }
  }

  def appendAll(outputs: Seq[ModelField], inputs: Seq[ModelField]): Option[Seq[ModelField]] = {
    val fields = inputs.map { input =>
      outputs.find(_.name == input.name).flatMap { output =>
        merge(output, input)
      }
    }

    if (fields.exists(_.isEmpty)) {
      None
    } else {
      Some(fields.flatten)
    }
  }

}

object ModelFieldOps extends ModelFieldOps