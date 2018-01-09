package io.hydrosphere.serving.manager.service.prometheus

import io.hydrosphere.serving.manager.connector.EnvoyAdminConnector
import io.hydrosphere.serving.manager.model.ModelServiceInstance
import io.hydrosphere.serving.manager.service.RuntimeManagementService
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

case class ServiceTargetLabels(
  job: String,
  modelName: String,
  modelVersion: Long,
  serviceId: String,
  instanceId: String,
  serviceName: String
)

case class ServiceTargets(
  targets: List[String],
  labels: ServiceTargetLabels
)

trait PrometheusMetricsService {
  def fetchServices(): Future[Seq[ServiceTargets]]

  def fetchMetrics(serviceId: Long, instanceId: String): Future[String]
}

class PrometheusMetricsServiceImpl(
  runtimeManagementService: RuntimeManagementService,
  envoyAdminConnector: EnvoyAdminConnector
)(implicit val ex: ExecutionContext) extends PrometheusMetricsService with Logging {

  override def fetchServices(): Future[Seq[ServiceTargets]] =
    runtimeManagementService.allServices().flatMap(services => {
      Future.traverse(services)(s =>
        runtimeManagementService.instancesForService(s.serviceId)
          .map(inst => Tuple2(s, inst))
      ).map(allServices => {
        val clusters = mutable.MutableList[ServiceTargets]()
        allServices.foreach(tuple => {
          tuple._2.foreach(instance => {
            clusters += ServiceTargets(
              labels = ServiceTargetLabels(
                job = tuple._1.serviceName,
                modelName = tuple._1.modelRuntime.modelName,
                modelVersion = tuple._1.modelRuntime.modelVersion,
                serviceId = tuple._1.serviceId.toString,
                instanceId = instance.instanceId,
                serviceName = tuple._1.serviceName
              ),
              targets = List(s"${instance.host}:${instance.sidecarAdminPort}")
            )
          })
        })
        clusters
      })
    })

  override def fetchMetrics(serviceId: Long, instanceId: String): Future[String] =
    runtimeManagementService.instancesForService(serviceId)
      .flatMap(instances => {
        instances.find(i => i.instanceId == instanceId) match {
          case Some(x) =>
            fetchAndMapMetrics(x)
          case _ =>
            throw new IllegalArgumentException(s"Can't find instance=$instanceId in service=$serviceId")
        }
      })

  private case class PrometheusMetric(
    name: String,
    metricType: String,
    help: String
  )

  private case class PrometheusMetricValue(
    groups: Map[String, String],
    value: String
  )

  private def fetchAndMapMetrics(instance: ModelServiceInstance): Future[String] =
    envoyAdminConnector.stats(instance.host, instance.sidecarAdminPort)
      .map(response => mapToPrometheusMetrics(response))


  private def mapToPrometheusMetrics(envoyMetrics: String): String = {
    val map = scala.collection.mutable.HashMap.empty[PrometheusMetric, mutable.MutableList[PrometheusMetricValue]]

    envoyMetrics.split("\n").foreach(metricRow => {
      val tuple = parseEnvoyMetric(metricRow)
      val list = map.getOrElseUpdate(tuple._1, mutable.MutableList.empty)
      list += tuple._2
    })

    val builder = new StringBuilder()
    map.foreach(tuple => {
      formatToPrometheus(tuple._1, tuple._2, builder)
    })
    builder.toString()
  }

  //TODO add this to envoy
  private def formatToPrometheus(prometheusMetric: PrometheusMetric, values: mutable.MutableList[PrometheusMetricValue], builder: StringBuilder) = {
    builder.append("# HELP ")
    builder.append(prometheusMetric.name)
    builder.append(" ")
    builder.append(prometheusMetric.help)
    builder.append("\n")

    builder.append("# TYPE ")
    builder.append(prometheusMetric.name)
    builder.append(" ")
    builder.append(prometheusMetric.metricType)
    builder.append("\n")

    values.foreach(value => {
      builder.append(prometheusMetric.name)
      if (value.groups.nonEmpty) {
        builder.append(value.groups
          .map(t => t._1 + "=\"" + t._2 + "\"")
          .mkString("{", ",", "}")
        )
      }
      builder.append(" ")
      builder.append(value.value)
      builder.append("\n")
    })

  }

  private def parseEnvoyMetric(metricRow: String): (PrometheusMetric, PrometheusMetricValue) = {
    val arr = metricRow.split(':')
    val name = arr.head
    val sp = name.split('.')
    //TODO refactor
    var prometheusMetric = PrometheusMetric(name = sp.mkString("_").replaceAll("-", "_"), metricType = "counter", help = name)
    var groups = immutable.Map[String, String]()
    if (name.startsWith("http.async-client.")) {
      //Do nothing
    } else if (name.startsWith("http.") || name.startsWith("cluster.")) {
      val newName = (sp(0) + "_" + sp.drop(2).mkString("_")).replaceAll("-", "_")
      prometheusMetric = PrometheusMetric(name = newName, metricType = "counter", help = newName)
      groups = immutable.Map(sp(0) -> sp(1))
    } else if (name.startsWith("listener.")) {
      var index = name.indexOf(".downstream")
      if (-1 == index) {
        index = name.indexOf(".server")
      }
      val newName = ("listener_" + name.substring(index + 1).replaceAll("-", "_")).replaceAll("\\.", "_")
      prometheusMetric = PrometheusMetric(name = newName, metricType = "counter", help = newName)
    } else if (name.startsWith("server.")) {
      prometheusMetric = PrometheusMetric(name = sp.mkString("_"), metricType = "gauge", help = name)
    }
    Tuple2(prometheusMetric, PrometheusMetricValue(groups, arr.last))
  }
}