package io.hydrosphere.serving.manager.service.metrics

import java.time.LocalDate

import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.BasicFieldDefinition
import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.connector.EnvoyAdminConnector
import io.hydrosphere.serving.manager.model.db.{Service, ServiceKeyDescription}
import io.hydrosphere.serving.manager.service.actors.SelfScheduledActor
import io.hydrosphere.serving.manager.service.actors.SelfScheduledActor.Tick
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.clouddriver.{CloudDriverService, MetricServiceTargetLabels, MetricServiceTargets}
import io.hydrosphere.serving.manager.service.service.ServiceManagementService

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

class ElasticSearchMetricsService(
    managerConfiguration: ManagerConfiguration,
    envoyAdminConnector: EnvoyAdminConnector,
    cloudDriverService: CloudDriverService,
    serviceManagementService: ServiceManagementService,
    applicationManagementService: ApplicationManagementService,
    elasticClient: HttpClient
) extends SelfScheduledActor(
  initialDelay=managerConfiguration.metrics.elasticSearch.get.collectTimeout.seconds,
  interval=managerConfiguration.metrics.elasticSearch.get.collectTimeout.seconds
)(30.seconds) {

  private implicit val ex: ExecutionContext = context.system.dispatcher

  private val elasticConfig = managerConfiguration.metrics.elasticSearch.get

  def createIndexIfNeeded(): Unit =
    elasticClient.execute {
      indexExists(elasticConfig.indexName)
    }.await.right.map(r => {
      if (!r.result.exists) {
        elasticClient.execute {
          createIndex(elasticConfig.indexName).mappings(
            mapping(elasticConfig.mappingName).fields(
              dateField("@timestamp"),
              textField("name"),
              textField("hostIp"),
              BasicFieldDefinition(
                name = "value",
                `type` = "double",
                index = Some(false)
              )
            )
          )
        }.await
      }
    })

  override def onTick(): Unit = {
    try {
      createIndexIfNeeded()

      applicationManagementService.allApplications().flatMap { apps =>
        val keys = for {
          app <- apps
          stage <- app.executionGraph.stages
          service <- stage.services
        } yield {
          service.serviceDescription
        }
        //TODO cache this
        serviceManagementService.fetchServicesUnsync(keys.toSet)
          .flatMap { services =>
            val map = services.map(s => s.toServiceKeyDescription.toServiceName() -> s).toMap

            cloudDriverService.getMetricServiceTargets()
              .flatMap(p => pushMetricsToTargets(p, map))
          }
      }
    } catch {
      case e: Exception =>
        log.error(e, e.getMessage)
    }
  }

  private def pushMetricsToTargets(seq: Seq[MetricServiceTargets], map: Map[String, Service]): Future[Unit] =
    Future.sequence(
      seq.filter(_.labels.serviceType.exists(_.equals(CloudDriverService.DEPLOYMENT_TYPE_SIDECAR)))
        .flatMap(p => p.targets.map(t => {
          fetchAndSendMetrics(t, p.labels, map)
        }))
    ).map(_ => Unit)


  private def fetchAndSendMetrics(target: String, labels: MetricServiceTargetLabels, map: Map[String, Service]): Future[Unit] = {
    val hostAndPort = target.split(":")
    val host = hostAndPort.head
    val port = hostAndPort.last.toInt

    envoyAdminConnector.stats(host, port)
      .flatMap(s => mapAndPushMetricsToElastic(s, host, labels, map))
  }

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

  private def metricToJson(metric: EnvoyMetrics, currentTimestamp: Long): Iterable[(String, Any)] = {
    metric.labels.toSeq :+ ("name" -> metric.name) :+ ("value" -> metric.value) :+ ("@timestamp" -> currentTimestamp)
  }

  private def mapAndPushMetricsToElastic(metrics: String, targetHost: String, labels: MetricServiceTargetLabels, map: Map[String, Service]): Future[Unit] = {
    val metricsSeq = parseAllMetrics(metrics)
      .map(m => addTagsIfNeeded(m, targetHost, labels, map))

    val date = System.currentTimeMillis()
    val localDate=LocalDate.now()

    elasticClient.execute {
      bulk(
        metricsSeq.map(metric => {
          indexInto(elasticConfig.indexName / s"elasticConfig.mappingName_${localDate.getYear}_${localDate.getMonthValue}_${localDate.getDayOfMonth}")
            .fields(metricToJson(metric, date))
        })
      )
    }.map(p => Unit)
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

  private case class EnvoyMetrics(
      name: String,
      labels: Map[String, String],
      value: Long
  )

  override def recieveNonTick: Receive = {
    case _ =>
  }
}
