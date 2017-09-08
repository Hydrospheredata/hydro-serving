package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.ModelFile

import scala.concurrent.Future

trait ModelFilesRepository extends BaseRepository[ModelFile, Long] {
  def deleteModelFiles(modelId: Long): Future[Int]

  def get(filePath: String): Future[Option[ModelFile]]

  def update(modelFile: ModelFile): Future[Int]

  def modelFiles(modelId: Long): Future[List[ModelFile]]
}
