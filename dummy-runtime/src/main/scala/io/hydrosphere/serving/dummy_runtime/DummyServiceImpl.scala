package io.hydrosphere.serving.dummy_runtime

import java.time.LocalDateTime

import com.google.protobuf.ByteString
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc.PredictionService
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future

class DummyServiceImpl extends PredictionService with Logging {

  import scala.concurrent.ExecutionContext.Implicits._

  override def predict(request: PredictRequest): Future[PredictResponse] = {
    Future {
      logger.info(s"Incoming request: $request")
      val tensorData = Seq(
        ByteString.copyFromUtf8(LocalDateTime.now().toString)
      )
      val response = PredictResponse(
        request.inputs ++ Map(
          "dummy_result" -> TensorProto(DataType.DT_STRING, None, stringVal = tensorData)
        )
      )
      logger.info(s"Response: $response")
      response
    }
  }
}
