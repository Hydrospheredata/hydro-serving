package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.model.Model
import io.hydrosphere.serving.manager.model.api.{ModelMetadata, ModelType}
import io.hydrosphere.serving.manager.repository.ModelRepository
import org.mockito.{Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Success

class ModelManagementSpec extends GenericUnitTest with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  "Model management service" should "index deleted models" in {
    val modelRepo = mock[ModelRepository]
    val sourceMock = mock[SourceManagementService]

    Mockito.when(sourceMock.index("local:test1")).thenReturn(
      Future.successful(
        Success(None)
      )
    )
    Mockito.when(modelRepo.delete(1L)).thenReturn(Future.successful(1))
    Mockito.when(modelRepo.getMany(Set(1L))).thenReturn(
      Future.successful(
        Seq(
          Model(
            id = 1,
            name = "test1",
            source = "local:test1",
            modelType = ModelType.Unknown("test"),
            description = None,
            modelContract = ModelContract.defaultInstance,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
          )
        )
      )
    )

    val modelManagementService = new ModelManagementServiceImpl(modelRepo, null, null, null, null, null, sourceMock)

    val f = modelManagementService.indexModels(Set(1L)).map { statuses =>
      println(statuses)
      assert(statuses.nonEmpty)
      assert(!statuses.exists(_.isInstanceOf[IndexError]))
      assert(statuses.forall(_.isInstanceOf[ModelDeleted]))
    }

    Await.result(f, 10 seconds)
  }

  it should "index updated models" in {
    val modelRepo = mock[ModelRepository]
    val sourceMock = mock[SourceManagementService]

    Mockito.when(sourceMock.index("local:test1")).thenReturn(
      Future.successful(
        Success(Some(
          ModelMetadata(
            "newModel",
            ModelType.Unknown("test2"),
            ModelContract.defaultInstance.copy(modelName = "newmodel")
          )
        ))
      )
    )
    Mockito.when(modelRepo.update(Matchers.any(classOf[Model]))).thenReturn(Future.successful(1))
    Mockito.when(modelRepo.getMany(Set(1L))).thenReturn(
      Future.successful(
        Seq(
          Model(
            id = 1,
            name = "test1",
            source = "local:test1",
            modelType = ModelType.Unknown("test"),
            description = None,
            modelContract = ModelContract.defaultInstance,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
          )
        )
      )
    )

    val modelManagementService = new ModelManagementServiceImpl(modelRepo, null, null, null, null, null, sourceMock)

    val f = modelManagementService.indexModels(Set(1L)).map { statuses =>
      println(statuses)
      assert(statuses.nonEmpty)
      assert(!statuses.exists(_.isInstanceOf[IndexError]))
      assert(statuses.forall(_.isInstanceOf[ModelUpdated]))
    }

    Await.result(f, 10 seconds)
  }

  it should "upload model" in {
    pending
  }

  it should "add model" in {
    pending
  }
}
