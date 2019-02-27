package io.hydrosphere.serving.manager

import java.nio.file.{Path, Paths}

import cats.data.EitherT
import cats.effect.IO
import io.hydrosphere.serving.manager.domain.DomainError
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, AsyncFunSpecLike, Matchers}

import scala.concurrent.Future

trait GenericUnitTest extends AsyncFunSpecLike with Matchers with MockitoSugar {
  def when[T](methodCall: T) = Mockito.when(methodCall)

  protected def ioAssert(body : => IO[Assertion]): Future[Assertion] = {
    body.unsafeToFuture()
  }

  protected def eitherTAssert(body: => EitherT[IO, DomainError, Assertion]): Future[Assertion] = {
    eitherAssert(body.value)
  }

  protected def eitherAssert(body: => IO[Either[DomainError,Assertion]]): Future[Assertion] = {
    ioAssert {
      body.map {
        case Left(err) =>
          fail(err.message)
        case Right(asserts) =>
          asserts
      }
    }
  }

  protected def getTestResourcePath(path: String): Path = {
    Paths.get(this.getClass.getClassLoader.getResource(path).toURI)
  }
}