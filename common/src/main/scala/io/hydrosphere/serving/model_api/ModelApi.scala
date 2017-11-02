package io.hydrosphere.serving.model_api

import io.hydrosphere.serving.model_api.ModelApi.{MergeResult, TypeConflictError}


trait ModelApi

object ModelApi{
  trait MergeError {
    def msg: String

    override def toString: String = msg
  }

  case class TypeConflictError(model1: ModelField, model2: ModelField) extends MergeError {
    override def msg: String = s"Cannot merge fields: $model1 and $model2"
  }

  type MergeResult = Either[Seq[MergeError], ModelApi]

  def merge(modelApi1: ModelApi, modelApi2: ModelApi): MergeResult = {
    modelApi1 -> modelApi2 match {
      case (UntypedAPI, x) => Right(x)
      case (x, UntypedAPI) => Right(x)
      case (DataFrame(def1), DataFrame(def2)) =>
        DataFrame.create(def1 ++ def2)
    }
  }
}

case class DataFrame(definition: List[ModelField]) extends ModelApi

object DataFrame {
  def create(modelFields: Seq[ModelField]): MergeResult = {
    val results = modelFields.groupBy(_.name).values.map{
      case field :: Nil => // Ok
        Right(field)
      case fields =>
        val firstField = fields.head
        var errors = List.empty[TypeConflictError]
        fields.foreach{ field =>
          if (field.fieldType != firstField.fieldType) {
            errors = errors :+ TypeConflictError(firstField, field)
          }
        }
        if (errors.isEmpty) {
          Right(firstField)
        } else {
          Left(errors)
        }
      case x => throw new IllegalArgumentException(s"Can't create DataFrame with $x")
    }.toList

    val errors = results.filter(_.isLeft).map(_.left.get)
    if (errors.isEmpty) {
      Right(DataFrame(results.map(_.right.get)))
    } else {
      Left(errors.flatten)
    }
  }
}

case object UntypedAPI extends ModelApi