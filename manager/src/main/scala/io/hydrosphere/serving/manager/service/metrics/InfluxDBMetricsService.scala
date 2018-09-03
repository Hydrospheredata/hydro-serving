package io.hydrosphere.serving.manager.service.metrics

import akka.actor.Props
import com.paulgoldbaum.influxdbclient.Parameter.Precision
import com.paulgoldbaum.influxdbclient.Parameter.Precision.Precision
import com.paulgoldbaum.influxdbclient._
import io.hydrosphere.serving.manager.config.ManagerConfiguration
import io.hydrosphere.serving.manager.connector.{EnvoyAdminConnector, HttpEnvoyAdminConnector}
import io.hydrosphere.serving.manager.model.db.Service
import io.hydrosphere.serving.manager.service.actors.SelfScheduledActor
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.clouddriver.{CloudDriverService, MetricServiceTargets}
import io.hydrosphere.serving.manager.service.service.ServiceManagementService

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class InfluxDBMetricsService(
    managerConfiguration: ManagerConfiguration,
    envoyAdminConnector: EnvoyAdminConnector,
    cloudDriverService: CloudDriverService,
    serviceManagementService: ServiceManagementService,
    applicationManagementService: ApplicationManagementService,
    influxDB: InfluxDB
) extends SelfScheduledActor(
  initialDelay = managerConfiguration.metrics.influxDb.get.collectTimeout.seconds,
  interval = managerConfiguration.metrics.influxDb.get.collectTimeout.seconds
)(30.seconds) with AbstractMetricService {

  override implicit val ex: ExecutionContext = context.system.dispatcher

  private val influxDBConfig = managerConfiguration.metrics.influxDb.get

  private def createIndexIfNeeded(): Future[Unit] = {
    val database = influxDB.selectDatabase(influxDBConfig.databaseName)
    database.exists().flatMap(f => {
      if (!f) {
        database.create().map(_ => Unit)
      } else {
        Future.successful(Unit)
      }
    })
  }

  override def onTick(): Unit = {
    try {
      createIndexIfNeeded().flatMap{ _ =>
        fetchServices(applicationManagementService, serviceManagementService, cloudDriverService)
          .flatMap { map =>
            cloudDriverService.getMetricServiceTargets()
              .flatMap(p => pushMetricsToTargets(p, map))
          }
      }
      .failed.foreach{ t =>
        log.error(t, t.getMessage)
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
            .flatMap(m => {
              mapAndPushMetricsToElastic(m)
            })
        }))
    ).map(_ => Unit)


  private def mapAndPushMetricsToElastic(metricsSeq: Seq[EnvoyMetrics]): Future[Unit] = {
    val date = System.currentTimeMillis()

    val database = influxDB.selectDatabase(influxDBConfig.databaseName)

    val mappedMetrics = metricsSeq.map(m => Point(
      key = m.name,
      timestamp = date,
      tags = m.labels.map(l => {
        Tag(l._1, l._2)
      }).toSeq,
      Seq(LongField("value", m.value))
    ))

    database.bulkWrite(mappedMetrics, Precision.MILLISECONDS).map(_ => Unit)
  }

  override def recieveNonTick: Receive = {
    case _ =>
  }
}

object InfluxDBMetricsService {
  def props(
    managerConfiguration: ManagerConfiguration,
    envoyAdminConnector: HttpEnvoyAdminConnector,
    cloudDriverService: CloudDriverService,
    serviceManagementService: ServiceManagementService,
    applicationManagementService: ApplicationManagementService,
    influxDBClient: InfluxDB
  ): Props = Props(classOf[InfluxDBMetricsService],
    managerConfiguration: ManagerConfiguration,
    envoyAdminConnector,
    cloudDriverService,
    serviceManagementService,
    applicationManagementService,
    influxDBClient
  )

}