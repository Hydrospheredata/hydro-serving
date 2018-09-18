package io.hydrosphere.serving.manager.service.source.fetchers.keras

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.service.source.fetchers.tensorflow.TypeMapper
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType

private[keras] sealed trait ModelConfig {
  def toSignatures: Seq[ModelSignature]
}

private[keras] object ModelConfig {

  import io.hydrosphere.serving.manager.util.SnakifiedSprayJsonSupport._
  import spray.json._


  case class FunctionalModel(config: FunctionalModelConfig) extends ModelConfig {
    override def toSignatures: Seq[ModelSignature] = {
      // first element of the array is layer name
      val inputNames = config.inputLayers.map(_.elements.head.asInstanceOf[JsString].value)
      val outputNames = config.outputLayers.map(_.elements.head.asInstanceOf[JsString].value)

      val inputLayers = config.layers.filter(l => inputNames.contains(l.name))
      val outputLayers = config.layers.filter(l => outputNames.contains(l.name))

      val inputs = inputLayers.map(_.config.field)
      val outputs = outputLayers.map(_.config.field)

      Seq(
        ModelSignature(
          signatureName = "infer",
          inputs = inputs,
          outputs = outputs
        )
      )
    }
  }
  case class FunctionalModelConfig(name: String, layers: List[FunctionalLayerConfig], inputLayers: List[JsArray], outputLayers: List[JsArray])
  case class FunctionalLayerConfig(name: String, className: String, config: LayerConfig, inboundNodes: JsArray)

  case class SequentialModel(config: List[SequentialLayerConfig]) extends ModelConfig {
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
  case class SequentialLayerConfig(className: String, config: LayerConfig)

  case class LayerConfig(name: String, dtype: Option[String], batchInputShape: Option[JsArray], units: Option[Long], targetShape: Option[JsArray]) {
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
      val ttype = dtype.map(TypeMapper.toType).getOrElse(DataType.DT_INVALID)
      ModelField(
        name = name,
        shape = getShape.toProto,
        typeOrSubfields = ModelField.TypeOrSubfields.Dtype(ttype)
      )
    }

  }


  implicit val layerConfigFormat = jsonFormat5(LayerConfig.apply)
  implicit val layerFormat = jsonFormat2(SequentialLayerConfig.apply)
  implicit val seqFormat = jsonFormat1(SequentialModel.apply)
  implicit val funcLayerConfig = jsonFormat4(FunctionalLayerConfig.apply)
  implicit val funcConfigFormat = jsonFormat4(FunctionalModelConfig.apply)
  implicit val funcFormat = jsonFormat1(FunctionalModel.apply)
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