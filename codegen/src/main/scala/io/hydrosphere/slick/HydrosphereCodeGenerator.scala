package io.hydrosphere.slick

import java.net.URI

import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration.Duration

import slick.codegen.SourceCodeGenerator
import slick.sql.SqlProfile.ColumnOption
import slick.basic.DatabaseConfig
import slick.{model => m}
import slick.util.ConfigExtensionMethods.configExtensionMethods

/**
  * This customizes the Slick code generator. We only do simple name mappings.
  * For a more advanced example see https://github.com/cvogt/slick-presentation/tree/scala-exchange-2013
  */
class HydrosphereCodeGenerator(model: m.Model) extends SourceCodeGenerator(model) {
  type HTable = HTableDef


  override def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]) : String = {
    s"""
package $pkg
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object $container extends {
  val profile = $profile
} with $container
/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait $container${parentType.map(t => s" extends $t").getOrElse("")} {
  val profile: $profile
  import profile.api._
  ${indent(code)}
}
      """.trim()
  }


  override def Table = new HTable(_)

  class HTableDef(model: m.Table) extends super.TableDef(model) {
    override type Column = ColumnDef

    override def Column = new Column(_) {

      override def rawType: String = {
        model.tpe match {
          case "java.sql.Date" => "java.time.LocalDate"
          case "java.sql.Time" => "java.time.LocalTime"
          case "java.sql.Timestamp" => "java.time.LocalDateTime"
          case "scala.collection.Seq" => model.options.find(_.isInstanceOf[ColumnOption.SqlType]).map(_.asInstanceOf[ColumnOption.SqlType].typeName).map({
            case "_text" => "List[String]"
            case _ => "List[String]"
          }).getOrElse("String")
          case _ => super.rawType
        }
      }
    }
  }

}

object HydrosphereCodeGenerator {

  def run(profile: String, jdbcDriver: String, url: String, outputDir: String, pkg: String, user: Option[String], password: Option[String], ignoreInvalidDefaults: Boolean): Unit = {
    val profileInstance: HydrospherePostgresDriver =
      Class.forName(profile + "$").getField("MODULE$").get(null).asInstanceOf[HydrospherePostgresDriver]
    val dbFactory = profileInstance.api.Database
    val db = dbFactory.forURL(url, driver = jdbcDriver,
      user = user.orNull, password = password.orNull, keepAliveConnection = true)
    try {
      val m = Await.result(db.run(profileInstance.createModel(None, ignoreInvalidDefaults)(ExecutionContext.global).withPinnedSession), Duration.Inf)
      new HydrosphereCodeGenerator(m).writeToFile(profile, outputDir, pkg)
    } finally db.close
  }

  def run(uri: URI, outputDir: Option[String], ignoreInvalidDefaults: Boolean = true): Unit = {
    val dc = DatabaseConfig.forURI[HydrospherePostgresDriver](uri)
    val pkg = dc.config.getString("codegen.package")
    val out = outputDir.getOrElse(dc.config.getStringOr("codegen.outputDir", "."))
    val profile = if (dc.profileIsObject) dc.profileName else "new " + dc.profileName
    try {
      val m = Await.result(dc.db.run(dc.profile.createModel(None, ignoreInvalidDefaults)(ExecutionContext.global).withPinnedSession), Duration.Inf)
      new HydrosphereCodeGenerator(m).writeToFile(profile, out, pkg)
    } finally dc.db.close
  }

  def main(args: Array[String]): Unit = {
    args.toList match {
      case uri :: Nil =>
        run(new URI(uri), None)
      case uri :: outputDir :: Nil =>
        run(new URI(uri), Some(outputDir))
      case profile :: jdbcDriver :: url :: outputDir :: pkg :: Nil =>
        run(profile, jdbcDriver, url, outputDir, pkg, None, None, ignoreInvalidDefaults = true)
      case profile :: jdbcDriver :: url :: outputDir :: pkg :: user :: password :: Nil =>
        run(profile, jdbcDriver, url, outputDir, pkg, Some(user), Some(password), ignoreInvalidDefaults = true)
      case profile :: jdbcDriver :: url :: outputDir :: pkg :: user :: password :: ignoreInvalidDefaults :: Nil =>
        run(profile, jdbcDriver, url, outputDir, pkg, Some(user), Some(password), ignoreInvalidDefaults.toBoolean)
      case _ =>
        println(
          """
            |Usage:
            |  SourceCodeGenerator configURI [outputDir]
            |  SourceCodeGenerator profile jdbcDriver url outputDir pkg [user password]
            |
            |Options:
            |  configURI: A URL pointing to a standard database config file (a fragment is
            |    resolved as a path in the config), or just a fragment used as a path in
            |    application.conf on the class path
            |  profile: Fully qualified name of Slick profile class, e.g. "slick.jdbc.H2Profile"
            |  jdbcDriver: Fully qualified name of jdbc driver class, e.g. "org.h2.Driver"
            |  url: JDBC URL, e.g. "jdbc:postgresql://localhost/test"
            |  outputDir: Place where the package folder structure should be put
            |  pkg: Scala package the generated code should be places in
            |  user: database connection user name
            |  password: database connection password
            |
            |When using a config file, in addition to the standard config parameters from
            |slick.basic.DatabaseConfig you can set "codegen.package" and
            |"codegen.outputDir". The latter can be overridden on the command line.
          """.stripMargin.trim)
        System.exit(1)
    }
  }
}
