package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.ModelFile

import scala.concurrent.Future

trait ModelFilesRepository extends BaseRepository[ModelFile, Long] {
  def get(filePath: String): Future[Option[ModelFile]]

  def update(modelFile: ModelFile): Future[ModelFile]

  def modelFiles(modelId: Long): List[ModelFile]
}
