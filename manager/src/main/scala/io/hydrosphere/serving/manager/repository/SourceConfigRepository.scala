package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.ModelSourceConfiguration
import io.hydrosphere.serving.manager.service.modelsource.ModelSource

trait SourceConfigRepository extends BaseRepository[ModelSourceConfiguration, Long] {

}
