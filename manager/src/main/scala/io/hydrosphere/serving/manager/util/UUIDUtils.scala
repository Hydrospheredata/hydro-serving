package io.hydrosphere.serving.manager.util

import java.util.UUID

object UUIDUtils {
  final val zerosStr = "00000000-0000-0000-0000-000000000000"
  final val zeros = UUID.fromString(zerosStr)
}
