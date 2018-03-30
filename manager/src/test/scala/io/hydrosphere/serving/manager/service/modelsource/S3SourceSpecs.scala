package io.hydrosphere.serving.manager.service.modelsource

import io.hydrosphere.serving.manager.service.modelsource.s3.{S3ModelSource, S3SourceDef}

class S3SourceSpecs extends ModelSourceSpec(S3SourceSpecs.source) {
  pending

  override def getSourceFile(path: String): String = ???
}

object S3SourceSpecs {
  val source = new S3ModelSource(new S3SourceDef(null, null, null, null))
}