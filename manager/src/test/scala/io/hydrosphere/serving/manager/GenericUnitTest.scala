package io.hydrosphere.serving.manager

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpecLike, Matchers}

import scala.concurrent.duration._

trait GenericUnitTest extends AsyncFlatSpecLike with Matchers with MockitoSugar {
}
