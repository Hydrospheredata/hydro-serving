package io.hydrosphere.serving.manager

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpecLike, Matchers}
import scala.concurrent.duration._

trait GenericUnitTest extends FlatSpecLike with Matchers with MockitoSugar {
  def futureTimeout = 30 seconds
}
