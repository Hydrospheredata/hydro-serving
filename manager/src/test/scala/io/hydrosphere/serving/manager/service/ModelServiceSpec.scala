package io.hydrosphere.serving.manager.service

import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model.Result
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.manager.repository.ModelRepository
import io.hydrosphere.serving.manager.service.contract.ContractUtilityService
import io.hydrosphere.serving.manager.service.model.ModelManagementServiceImpl
import io.hydrosphere.serving.manager.service.source.{ModelStorageService, StorageUploadResult}
import io.hydrosphere.serving.manager.util.TarGzUtils
import org.mockito.{Matchers, Mockito}

import scala.concurrent.Future

class ModelServiceSpec extends GenericUnitTest {
  val contractSerice = mock[ContractUtilityService]
  private[this] val dummyModel = Model(
    id = 1,
    name = "/test_models/tensorflow_model",
    modelType = ModelType.Unknown("test"),
    description = None,
    modelContract = ModelContract.defaultInstance,
    created = LocalDateTime.now(),
    updated = LocalDateTime.now()
  )
  import scala.concurrent.ExecutionContext.Implicits._

  def packModel(str: String): Path = {
    val temptar = Files.createTempFile("test_tf_model", ".tar.gz")
    TarGzUtils.compress(Paths.get(getClass.getResource(str).getPath), temptar, None)
    temptar
  }

  "Model management service" should "upload new model" in {
    val testSourcePath = Files.createTempDirectory("upload-test").toString
    println("Test source path: " + testSourcePath)
    val upload = ModelUpload(
      packModel("/test_models/tensorflow_model/saved_model.pb"),
      Some("tf-model"),
      Some("unknown:unknown"),
      None,
      None,
      None
    )
    println(upload)
    val model = Model(
      id = 1,
      name = "tf-model",
      modelType = ModelType.Tensorflow(),
      description = None,
      modelContract = ModelContract.defaultInstance,
      created = LocalDateTime.now(),
      updated = LocalDateTime.now()
    )
    val modelRepo = mock[ModelRepository]
    val sourceMock = mock[ModelStorageService]

    Mockito.when(sourceMock.upload(Matchers.any())).thenReturn(
      Result.okF(StorageUploadResult(
        "tf-model",
        ModelType.Tensorflow(),
        None,
        ModelContract("tf-model", Seq(ModelSignature()))
      ))
    )
    Mockito.when(modelRepo.get("tf-model")).thenReturn(Future.successful(None))
    Mockito.when(modelRepo.create(Matchers.any())).thenReturn(
      Future.successful(model)
    )

    val modelManagementService = new ModelManagementServiceImpl(modelRepo, null, sourceMock, contractSerice)

    modelManagementService.uploadModel(upload).map { maybeModel =>
      maybeModel.isRight should equal(true)
      val rModel = maybeModel.right.get
      println(rModel)
      rModel.name should equal("tf-model")
    }
  }

  it should "upload existing model" in {
    val upload = ModelUpload(
      packModel("/test_models/tensorflow_model"),
      Some("test"),
      Some("unknown:unknown"),
      Some(ModelContract.defaultInstance),
      None,
      None
    )
    println(upload)
    val model = Model(
      id = 1,
      name = "tf-model",
      modelType = ModelType.Tensorflow(),
      description = None,
      modelContract = ModelContract.defaultInstance,
      created = LocalDateTime.now(),
      updated = LocalDateTime.now()
    )
    val modelRepo = mock[ModelRepository]
    val sourceMock = mock[ModelStorageService]

    Mockito.when(sourceMock.upload(Matchers.any())).thenReturn(
      Result.okF(StorageUploadResult(
        "tf-model",
        ModelType.Tensorflow(),
        None,
        ModelContract("tf-model", Seq(ModelSignature()))
      ))
    )
    Mockito.when(modelRepo.update(Matchers.any(classOf[Model]))).thenReturn(Future.successful(1))
    Mockito.when(modelRepo.get("tf-model")).thenReturn(Future.successful(Some(model)))
    Mockito.when(modelRepo.get(1)).thenReturn(Future.successful(Some(model)))

    val modelManagementService = new ModelManagementServiceImpl(modelRepo, null, sourceMock, contractSerice)

    modelManagementService.uploadModel(upload).map { maybeModel =>
      maybeModel.isRight should equal(true)
      val rModel = maybeModel.right.get
      rModel.name should equal("tf-model")
    }
  }
}