package io.hydrosphere.serving.manager.util

import cats.effect.Async

import scala.concurrent.{ExecutionContext, Future}

object AsyncUtil {
  def futureAsync[F[_] : Async, T](future: => Future[T])(implicit ec: ExecutionContext): F[T] = {
    Async[F].async[T] { cb =>
      future.onComplete(x => cb(x.toEither))
    }
  }
}