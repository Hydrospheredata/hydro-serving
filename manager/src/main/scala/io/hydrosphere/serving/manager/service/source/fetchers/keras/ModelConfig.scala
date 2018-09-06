package io.hydrosphere.serving.manager.service.source.fetchers.keras

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.service.source.fetchers.tensorflow.TypeMapper
import io.hydrosphere.serving.tensorflow.TensorShape

private[keras] sealed trait ModelConfig {
  def toSignatures: Seq[ModelSignature]
}

private[keras] object ModelConfig {

  import io.hydrosphere.serving.manager.util.SnakifiedSprayJsonSupport._
  import spray.json._

  case class SequentialModel(config: List[ModelLayer]) extends ModelConfig {
    override def toSignatures: Seq[ModelSignature] = {
      val firstLayer = config.head
      val lastLayer = config.last
      val input = firstLayer.config.field
      val output = lastLayer.config.field

      Seq(
        ModelSignature(
          signatureName = "infer",
          inputs = Seq(input),
          outputs = Seq(output)
        )
      )
    }
  }

  case class FunctionalModel(config: List[ModelLayer], inputs: JsValue, outputs: JsValue) extends ModelConfig {
    override def toSignatures: Seq[ModelSignature] = ???
  }

  case class LayerConfig(name: String, dtype: String, batchInputShape: Option[JsArray], units: Option[Long], targetShape: Option[JsArray]) {
    def getShape = {
      val arrDims = batchInputShape.orElse(targetShape).map { arr =>
        if (arr.elements.isEmpty) {
          TensorShape.scalar
        } else {
          val dims = arr.elements.map {
            case JsNumber(num) => num.toLong
            case _ => -1
          }
          TensorShape.mat(dims: _*)
        }
      }
      val scalarDims = units.map( u => TensorShape.Dims(Seq(-1, u)))

      TensorShape(arrDims.orElse(scalarDims).flatMap(_.toProto))
    }

    def field: ModelField = {
      ModelField(
        name = name,
        shape = getShape.toProto,
        typeOrSubfields = ModelField.TypeOrSubfields.Dtype(TypeMapper.toType(dtype))
      )
    }

  }

  case class ModelLayer(className: String, config: LayerConfig)


  implicit val layerConfigFormat = jsonFormat5(LayerConfig.apply)
  implicit val layerFormat = jsonFormat2(ModelLayer.apply)
  implicit val seqFormat = jsonFormat1(SequentialModel.apply)
  implicit val funcFormat = jsonFormat3(FunctionalModel.apply)
  implicit val configFormat = new RootJsonReader[ModelConfig] {
    override def read(json: JsValue): ModelConfig = {
      json match {
        case JsObject(fields) =>
          fields.get("class_name") match {
            case Some(className) =>
              val remainingFields = fields - "class_name"
              val remaningObj = JsObject(remainingFields)
              className match {
                case JsString("Model") => remaningObj.convertTo[FunctionalModel]
                case JsString("Sequential") => remaningObj.convertTo[SequentialModel]
                case x => throw DeserializationException(s"Unknown model class: $x")
              }
            case None => throw DeserializationException(s"Invalid ModelConfig json: $json")
          }
        case x => throw DeserializationException(s"$x is not a ModelConfig")
      }
    }
  }
}