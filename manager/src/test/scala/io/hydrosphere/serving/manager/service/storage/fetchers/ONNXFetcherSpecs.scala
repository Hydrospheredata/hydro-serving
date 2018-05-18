package io.hydrosphere.serving.manager.service.storage.fetchers

import java.nio.file.Paths

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.manager.model.api.ModelType.ONNX
import io.hydrosphere.serving.manager.service.source.fetchers.ONNXFetcher
import io.hydrosphere.serving.manager.service.source.storages.local.{LocalModelStorage, LocalModelStorageDefinition}
import io.hydrosphere.serving.manager.{GenericUnitTest, TestConstants}
import io.hydrosphere.serving.tensorflow.types.DataType

class ONNXFetcherSpecs extends GenericUnitTest {
  val localSource = new LocalModelStorage(LocalModelStorageDefinition("TEST", Paths.get(TestConstants.localModelsPath)))

  "ONNX fetcher" should "parse ONNX model" in {
    val expectedContract = ModelContract(
      "mnist",
      Seq(ModelSignature(
        "infer",
        Seq(
          ContractBuilders.simpleTensorModelField("Input73", DataType.DT_FLOAT, Some(Seq(1,1,28,28)))
        ),
        Seq(
          ContractBuilders.simpleTensorModelField("Plus422_Output_0", DataType.DT_FLOAT, Some(Seq(1,10)))
        ))
      )
    )

    val fetchResult = ONNXFetcher.fetch(localSource, "onnx_mnist")
    assert(fetchResult.isDefined, fetchResult)
    val metadata = fetchResult.get
    println(metadata)
    assert(metadata.modelName === "mnist")
    assert(metadata.modelType === ONNX("CNTK", "2.4"))
    assert(metadata.contract === expectedContract)
  }
}
