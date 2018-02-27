package io.hydrosphere.serving.manager.model.api.ops

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_field.ModelField.InfoOrSubfields.{Empty, Info, Subfields}
import io.hydrosphere.serving.manager.model.api.ContractBuilders
import io.hydrosphere.serving.manager.model.api.description.FieldDescription

trait ModelFieldOps {

  implicit class ModelFieldPumped(modelField: ModelField) {

    def flatten(rootName: String = ""): Seq[FieldDescription] = {
      ModelFieldOps.flatten(rootName, modelField)
    }

    def insert(name: String, fieldInfo: ModelField): Option[ModelField] = {
      modelField.infoOrSubfields match {
        case Subfields(fields) =>
          fields.data.find(_.fieldName == name) match {
            case Some(_) =>
              None
            case None =>
              val newData = fields.data :+ fieldInfo
              Some(ContractBuilders.complexField(modelField.fieldName, newData))
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

}

object ModelFieldOps {
  def merge(inputs: Seq[ModelField], inputs1: Seq[ModelField]): Seq[ModelField] = {
    (inputs, inputs1) match {
      case (Nil, x) => x
      case (x, Nil) => x
      case (x, y) if x == y => x
      case _ => inputs.zip(inputs1).flatMap {
        case (in1, in2) =>
          if (in1.fieldName == in2.fieldName) {
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
