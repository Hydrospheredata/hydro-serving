package io.hydrosphere.serving.manager.domain.metrics

import io.hydrosphere.serving.manager.domain.application.ApplicationService
import io.hydrosphere.serving.manager.domain.service.{Service, ServiceManagementService}
import io.hydrosphere.serving.manager.infrastructure.envoy.EnvoyAdminConnector
import io.hydrosphere.serving.manager.domain.clouddriver.{CloudDriverAlgebra, MetricServiceTargetLabels}

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
    applicationManagementService: ApplicationService,
    serviceManagementService: ServiceManagementService,
    cloudDriverService: CloudDriverAlgebra[Future]
  ): Future[Map[String, Service]] =
    applicationManagementService.allApplications().flatMap { apps =>
      val keys = for {
        app <- apps
        stage <- app.executionGraph.stages
        service <- stage.services
      } yield {
        service.modelVersion.id
      }
      serviceManagementService.fetchServicesUnsync(keys.toSet)
        .map(_.map(s => s.serviceName -> s).toMap)
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
      .flatMap(map.get)
      .map { service =>
        Map(
          "targetServiceId" -> service.id.toString,
          "targetRuntimeName" -> service.modelVersion.runtime.name,
          "targetRuntimeVersion" -> service.modelVersion.runtime.tag,
          "targetModelName" -> service.modelVersion.model.name,
          "targetModelVersion" -> service.modelVersion.modelVersion.toString,
          "targetEnvironment" -> service.modelVersion.hostSelector.map(_.name).getOrElse("_")
        )
      }.getOrElse(Map.empty)

    EnvoyMetrics(
      name = em.name,
      value = em.value,
      labels = em.labels ++ additionalLabels ++ Map("hostIp" -> targetHost)
    )
  }
}