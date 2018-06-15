package io.hydrosphere.serving.manager

import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFunSpecLike, Matchers}

trait GenericUnitTest extends AsyncFunSpecLike with Matchers with MockitoSugar {
  def when[T](methodCall: T) = Mockito.when(methodCall)
}
