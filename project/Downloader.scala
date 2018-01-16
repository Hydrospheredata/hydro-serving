
import sbt._

import scala.concurrent.Await

object Downloader {
  import scala.concurrent.ExecutionContext.Implicits._
  import scala.concurrent.duration._
  import gigahorse._
  import support.okhttp.Gigahorse

  def download(url: String, out: File): File = {
    val http = sbt.librarymanagement.Http.http
    val req = Gigahorse.url(url)
    Await.result(http.download(req, out), Duration.Inf)
  }
}
