package io.hydrosphere.serving.manager.service.metrics

import io.hydrosphere.serving.manager.connector.EnvoyAdminConnector
import io.hydrosphere.serving.manager.model.db.{Service, ServiceKeyDescription}
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.clouddriver.{CloudDriverService, MetricServiceTargetLabels}
import io.hydrosphere.serving.manager.service.service.ServiceManagementService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait AbstractMetricService {

  protected implicit val ex: ExecutionContext

  protected case class EnvoyMetrics(
      name: String,
      labels: Map[String, String],
      value: Long
  )

  protected def fetchMetrics(target: String, labels: MetricServiceTargetLabels, map: Map[String, Service], envoyAdminConnector: EnvoyAdminConnector): Future[Seq[EnvoyMetrics]] = {
    val hostAndPort = target.split(":")
    val host = hostAndPort.head
    val port = hostAndPort.last.toInt

    envoyAdminConnector.stats(host, port)
      .map(metrics => parseAllMetrics(metrics)
        .map(m => addTagsIfNeeded(m, host, labels, map)))
  }

  protected def fetchServices(
      applicationManagementService: ApplicationManagementService,
      serviceManagementService: ServiceManagementService,
      cloudDriverService: CloudDriverService
  ): Future[Map[String, Service]] =
    applicationManagementService.allApplications().flatMap { apps =>
      val keys = for {
        app <- apps
        stage <- app.executionGraph.stages
        service <- stage.services
      } yield {
        service.serviceDescription
      }
      serviceManagementService.fetchServicesUnsync(keys.toSet)
        .map(_.map(s => s.toServiceKeyDescription.toServiceName() -> s).toMap)
    }


  private def parseAllMetrics(metrics: String): Seq[EnvoyMetrics] =
    metrics.split("\n")
      .filterNot(_.startsWith("#"))
      .map(m => {

        val labelStart = m.indexOf('{')
        val labelEnd = if (labelStart != -1) {
          m.lastIndexOf('}')
        } else {
          -1
        }

        val metricLabels = if (labelStart != -1) {
          val lString = m.substring(labelStart + 1, labelEnd)
          lString.split(",")

            .map(label => {
              val lArr = label.split("=")
              val labelName = lArr.head
              val labelValue = lArr.lastOption.map(
                _.replaceAll("\"", "")
              )
              labelName -> labelValue.getOrElse("")
            })
            .filter(e => e._1.length > 0)
            .toMap
        } else {
          Map[String, String]()
        }

        val metricName = if (labelStart != -1) {
          m.substring(0, labelStart)
        } else {
          m.substring(0, m.indexOf(' '))
        }


        val trVal = Try(
          if (labelStart != -1) {
            m.substring(labelEnd + 1).trim.toLong
          } else {
            m.split(' ').last.toLong
          }
        )

        EnvoyMetrics(
          name = metricName,
          labels = metricLabels,
          value = trVal.getOrElse(-1)
        )
      })


  private def addTagsIfNeeded(em: EnvoyMetrics, targetHost: String, labels: MetricServiceTargetLabels, map: Map[String, Service]): EnvoyMetrics = {
    val additionalLabels = em.labels.get("envoy_cluster_name")
      .flatMap(d => {
        ServiceKeyDescription.fromServiceName(d).map(_ => {
          map.get(d).map(service => {
            Map(
              "targetServiceId" -> service.id.toString,
              "targetRuntimeName" -> service.runtime.name,
              "targetRuntimeVersion" -> service.runtime.version.toString,
              "targetModelName" -> service.model.map(_.modelName).getOrElse("_"),
              "targetModelVersion" -> service.model.map(_.modelVersion.toString).getOrElse("_"),
              "targetEnvironment" -> service.environment.map(_.name).getOrElse("_")
            )
          }).getOrElse(Map[String, String]())
        })
      }).getOrElse(Map[String, String]())

    EnvoyMetrics(
      name = em.name,
      value = em.value,
      labels = em.labels ++ additionalLabels ++ Map("hostIp" -> targetHost)
    )
  }
}
