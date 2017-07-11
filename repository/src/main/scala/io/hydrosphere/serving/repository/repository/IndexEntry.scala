package io.hydrosphere.serving.repository.repository

import io.hydrosphere.serving.repository.datasource.DataSource
import io.hydrosphere.serving.repository.ml.Model

/**
  * Created by Bulat on 02.06.2017.
  */
case class IndexEntry(model: Model, source: DataSource)
