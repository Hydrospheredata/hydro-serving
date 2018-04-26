package io.hydrosphere.serving.manager.service.metrics

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.BasicFieldDefinition
import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.connector.EnvoyAdminConnector
import io.hydrosphere.serving.manager.model.db.Service
import io.hydrosphere.serving.manager.service.actors.SelfScheduledActor
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.clouddriver.{CloudDriverService, MetricServiceTargets}
import io.hydrosphere.serving.manager.service.service.ServiceManagementService

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class ElasticSearchMetricsService(
    managerConfiguration: ManagerConfiguration,
    envoyAdminConnector: EnvoyAdminConnector,
    cloudDriverService: CloudDriverService,
    serviceManagementService: ServiceManagementService,
    applicationManagementService: ApplicationManagementService,
    elasticClient: HttpClient
) extends SelfScheduledActor(
  initialDelay = managerConfiguration.metrics.elasticSearch.get.collectTimeout.seconds,
  interval = managerConfiguration.metrics.elasticSearch.get.collectTimeout.seconds
)(30.seconds) with AbstractMetricService {

  override implicit val ex: ExecutionContext = context.system.dispatcher

  private val elasticConfig = managerConfiguration.metrics.elasticSearch.get

  def createIndexIfNeeded(): Unit = {
    val localDate = LocalDate.now()
    elasticClient.execute {
      indexExists(s"${elasticConfig.indexName}-${localDate.getYear}.${localDate.getMonthValue}.${localDate.getDayOfMonth}")
    }.await.right.map(r => {
      if (!r.result.exists) {
        elasticClient.execute {
          createIndex(s"${elasticConfig.indexName}-${localDate.format(DateTimeFormatter.BASIC_ISO_DATE)}").mappings(
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
  }

  override def onTick(): Unit = {
    try {
      createIndexIfNeeded()

      fetchServices(applicationManagementService, serviceManagementService, cloudDriverService)
        .flatMap { map =>
          cloudDriverService.getMetricServiceTargets()
            .flatMap(p => pushMetricsToTargets(p, map))
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
          fetchMetrics(t, p.labels, map, envoyAdminConnector)
            .flatMap(m=>{
              mapAndPushMetricsToElastic(m)
            })
        }))
    ).map(_ => Unit)


  private def metricToJson(metric: EnvoyMetrics, currentTimestamp: Long): Iterable[(String, Any)] = {
    metric.labels.toSeq :+ ("name" -> metric.name) :+ ("value" -> metric.value) :+ ("@timestamp" -> currentTimestamp)
  }


  private def mapAndPushMetricsToElastic(metricsSeq: Seq[EnvoyMetrics]): Future[Unit] = {
    val date = System.currentTimeMillis()
    val localDate = LocalDate.now()

    elasticClient.execute {
      bulk(
        metricsSeq.map(metric => {
          indexInto(s"${elasticConfig.indexName}-${localDate.format(DateTimeFormatter.BASIC_ISO_DATE)}" / elasticConfig.mappingName)
            .fields(metricToJson(metric, date))
        })
      )
    }.map(p => Unit)
  }

  override def recieveNonTick: Receive = {
    case _ =>
  }
}
