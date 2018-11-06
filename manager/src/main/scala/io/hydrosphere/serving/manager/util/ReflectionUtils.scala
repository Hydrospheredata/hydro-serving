package io.hydrosphere.serving.manager.util

import java.lang.reflect.Field

import scala.reflect.ClassTag
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

object ReflectionUtils  {
  def prettyPrint(a: Any): String = {
    // Recursively get all the fields; this will grab vals declared in parents of case classes.
    def getFields(cls: Class[_]): List[Field] =
      Option(cls.getSuperclass).map(getFields).getOrElse(Nil) ++
        cls.getDeclaredFields.toList.filterNot(f =>
          f.isSynthetic || java.lang.reflect.Modifier.isStatic(f.getModifiers))

    a match {
      // Make Strings look similar to their literal form.
      case s: String =>
        '"' + Seq("\n" -> "\\n", "\r" -> "\\r", "\t" -> "\\t", "\"" -> "\\\"", "\\" -> "\\\\").foldLeft(s) {
          case (acc, (c, r)) => acc.replace(c, r)
        } + '"'
      case xs: Array[_] =>
        s"Array(${xs.map(prettyPrint) mkString ", "})"
      case m: Map[_, _] =>
        m.map {
          case (k, v) => prettyPrint(k) -> prettyPrint(v)
        }.toString()
      case xs: Iterable[_] =>
        xs.map(prettyPrint).toString
      // This covers case classes.
      case p: Product =>
        s"${p.productPrefix}(${
          (getFields(p.getClass) map { f =>
            f setAccessible true
            s"${f.getName} = ${prettyPrint(f.get(p))}"
          }) mkString ", "
        })"
      // General objects and primitives end up here.
      case q =>
        Option(q).map(_.toString).getOrElse("¡null!")
    }
  }
}