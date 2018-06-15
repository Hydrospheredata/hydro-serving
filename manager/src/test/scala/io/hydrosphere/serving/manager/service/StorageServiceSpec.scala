package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.{GenericUnitTest, ManagerConfiguration}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{Matchers, Mockito}

import scala.concurrent.{Await, Future}

class StorageServiceSpec extends GenericUnitTest {

  import scala.concurrent.ExecutionContext.Implicits._

  describe("ModelStorage service") {
    it("should upload, unpack, and fetch a model tarball") {
      pending
    }
  }
}