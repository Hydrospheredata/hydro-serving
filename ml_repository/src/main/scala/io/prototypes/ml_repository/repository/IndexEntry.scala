package io.prototypes.ml_repository.repository

import io.prototypes.ml_repository.datasource.DataSource
import io.prototypes.ml_repository.ml.Model

/**
  * Created by Bulat on 02.06.2017.
  */
case class IndexEntry(model: Model, source: DataSource)
