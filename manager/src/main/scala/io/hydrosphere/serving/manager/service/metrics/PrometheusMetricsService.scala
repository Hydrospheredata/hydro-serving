package io.hydrosphere.serving.manager.service.metrics

import io.hydrosphere.serving.manager.connector.EnvoyAdminConnector
import io.hydrosphere.serving.manager.model.HFResult
import io.hydrosphere.serving.manager.model.db.{Service, ServiceKeyDescription}
import io.hydrosphere.serving.manager.service.clouddriver.{CloudDriverService, CloudService, MetricServiceTargets, ServiceInstance}
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.manager.model.Result
import Result.Implicits._
import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.service.ServiceManagementService

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}


trait PrometheusMetricsService {
  def fetchServices(): Future[Seq[MetricServiceTargets]]

  def fetchMetrics(serviceId: Long, instanceId: String, serviceType: String): HFResult[String]
}

class PrometheusMetricsServiceImpl(
  cloudDriverService: CloudDriverService,
  envoyAdminConnector: EnvoyAdminConnector,
  serviceManagementService: ServiceManagementService,
  applicationManagementService: ApplicationManagementService
)(implicit val ex: ExecutionContext) extends PrometheusMetricsService with Logging {

  override def fetchServices(): Future[Seq[MetricServiceTargets]] =
    cloudDriverService.getMetricServiceTargets()

  override def fetchMetrics(serviceId: Long, instanceId: String, serviceType: String): HFResult[String] = {
    val f = for {
      service <- extractService(serviceId)
      instance <- extractInstance(service, instanceId)
      serviceType <- fetchMetrics(service, instance, serviceType)
    } yield serviceType
    f.value
  }

  private def extractService(sid: Long) = {
    EitherT(cloudDriverService.
      services(Set(sid))
      .map(_.headOption.toHResult(ClientError(s"Can't find service=$sid"))))
  }

  private def extractInstance(service: CloudService, instanceId: String) = {
    val f = Future.successful(
      service.instances
        .find(_.instanceId == instanceId)
        .toHResult(ClientError(s"Can't find instance=$instanceId in service=$service"))
    )
    EitherT(f)
  }

  private def fetchMetrics(service: CloudService, instance: ServiceInstance, serviceType: String) = {
    val r = serviceType match {
      case CloudDriverService.DEPLOYMENT_TYPE_SIDECAR =>
        fetchAndMapMetrics(service, instance).map(Result.ok)
      case _ =>
        Result.clientErrorF(s"Illegal serviceType=$serviceType")
    }
    EitherT(r)
  }

  private def fetchAndMapMetrics(service: CloudService, instance: ServiceInstance): Future[String] =
    envoyAdminConnector.stats(instance.sidecar.host, instance.sidecar.adminPort)
      .flatMap(m => mapToPrometheusMetrics(m))

  private def changeLineIfNeeded(line: String, map: Map[String, Service]): String = {
    val searchString = "envoy_cluster_name=\""

    val index = line.lastIndexOf(searchString)
    if (index == -1) {
      line
    } else {
      val startIndex = index + searchString.length
      val clusterName = line.substring(startIndex, line.indexOf("\"", startIndex))

      ServiceKeyDescription.fromServiceName(clusterName) match {
        case None => line
        case Some(_) =>
          map.get(clusterName) match {
            case None => line
            case Some(service) =>
              val builder = new StringBuilder
              builder.append("targetServiceId=\"")
              builder.append(service.id)
              builder.append("\",targetRuntimeName=\"")
              builder.append(service.runtime.name)
              builder.append("\",targetRuntimeVersion=\"")
              builder.append(service.runtime.version)
              builder.append("\",targetModelName=\"")
              builder.append(service.model.map(_.modelName).getOrElse("_"))
              builder.append("\",targetModelVersion=\"")
              builder.append(service.model.map(_.modelVersion).getOrElse("_"))
              builder.append("\",targetEnvironment=\"")
              builder.append(service.environment.map(_.name).getOrElse("_"))
              builder.append("\",")

              line.substring(0, index) + builder.toString() + line.substring(index)
          }
      }
    }
  }

  private def mapToPrometheusMetrics(envoyMetrics: String): Future[String] = {
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
        .map { services =>
          val map = services.map(s => s.toServiceKeyDescription.toServiceName() -> s).toMap

          val metricsMap = new mutable.HashMap[String, mutable.ListBuffer[String]]()
          envoyMetrics.split("#").foreach { t =>
            val l = t.split("\n")

            l.drop(1).foreach { line =>
              val buff = metricsMap.getOrElseUpdate(l.head, new ListBuffer[String])
              buff += changeLineIfNeeded(line, map)
            }
          }

          // Because of https://github.com/envoyproxy/envoy/issues/2597
          val builder = new StringBuilder()
          metricsMap.foreach { f =>
            builder.append("#")
              .append(f._1)
              .append("\n")
            f._2.foreach { s =>
              builder.append(s)
                .append("\n")
            }
          }

          builder.toString()
        }
    }
  }
}