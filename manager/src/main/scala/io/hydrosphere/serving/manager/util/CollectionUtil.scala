package io.hydrosphere.serving.manager.util

import scala.collection.immutable.Iterable

object CollectionUtil {
  def maybeTransform[T, R](x: Iterable[T])(f: Iterable[T] => R): Option[R] = {
    if (x.isEmpty) None else Some(f(x))
  }
}