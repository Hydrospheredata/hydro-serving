package io.hydrosphere.serving.manager.domain.clouddriver
import akka.actor.ActorSystem
import akka.stream.Materializer
import cats.effect._
import cats.implicits._
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, DockerRepositoryConfiguration}
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging
import skuber.Container.PullPolicy
import skuber._
import skuber.apps.v1.{Deployment, DeploymentList}
import skuber.json.format._

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls

trait KubernetesClient[F[_]] {
  def services: F[List[skuber.Service]]
  def deployments: F[List[Deployment]]
  
  def runDeployment(name: String, servable: Servable, dockerImage: DockerImage): F[Deployment]
  def runService(name: String, servable: Servable): F[skuber.Service]
  
  def removeDeployment(name: String): F[Unit]
  def removeService(name: String): F[Unit]
}

object KubernetesClient {
  
  def apply[F[_]: Async](config: CloudDriverConfiguration.Kubernetes, dockerRepoConf: DockerRepositoryConfiguration.Remote)(implicit ex: ExecutionContext, actorSystem: ActorSystem, materializer: Materializer): KubernetesClient[F] = 
    KubernetesClient[F](
      config,
      dockerRepoConf,
      k8sInit(K8SConfiguration.useProxyAt(s"http://${config.proxyHost}:${config.proxyPort}")).usingNamespace(config.kubeNamespace)
    )

  def apply[F[_]: Async](config: CloudDriverConfiguration.Kubernetes, dockerRepoConf: DockerRepositoryConfiguration.Remote, underlying: K8SRequestContext)(implicit ex: ExecutionContext): KubernetesClient[F] = new KubernetesClient[F] with Logging {
    override def services: F[List[Service]] = {
      AsyncUtil.futureAsync(underlying.list[ServiceList]()).map(_.toList)
    }

    override def deployments: F[List[Deployment]] = {
      AsyncUtil.futureAsync(underlying.list[DeploymentList]).map(_.toList)
    }

    override def runDeployment(name: String, servable: Servable, dockerImage: DockerImage): F[Deployment] = {
      import LabelSelector.dsl._
      
      val dockerRepoHost = dockerRepoConf.pullHost.getOrElse(dockerRepoConf.host)
      val image = dockerImage.replaceUser(dockerRepoHost).toTry.get
      val pod = Pod.Template.Spec(
        metadata = ObjectMeta(name = servable.serviceName),
        spec = Some(Pod.Spec().addImagePullSecretRef(config.kubeRegistrySecretName))
      )
        .addContainer(Container("model", image.fullName).exposePort(DefaultConstants.DEFAULT_APP_PORT).withImagePullPolicy(PullPolicy.Always).setEnvVar(DefaultConstants.ENV_APP_PORT, DefaultConstants.DEFAULT_APP_PORT.toString))
        .addLabels(Map(
          CloudDriver.Labels.ServiceName -> name,
          CloudDriver.Labels.ModelVersionId -> servable.modelVersionId.toString
        ))
      val deployment = apps.v1.Deployment(metadata = ObjectMeta(name = servable.serviceName, labels = Map(
          CloudDriver.Labels.ServiceName -> name,
          CloudDriver.Labels.ModelVersionId -> servable.modelVersionId.toString
        )
      ))
        // TODO: make it configurable from api 
        .withReplicas(1)
        .withTemplate(pod)
        .withLabelSelector(CloudDriver.Labels.ServiceName is name)
      
      for {
        deployed <- deployments
        maybeExist  = deployed.find(_.metadata.labels.getOrElse(CloudDriver.Labels.ServiceName, "") == name)
        dpl <- maybeExist match {
          case Some(value) => Async[F].pure(value)
          case None => AsyncUtil.futureAsync(underlying.create(deployment))
        }
      } yield dpl
    }

    override def runService(name: String, servable: Servable): F[skuber.Service] = {
      val service = skuber.Service(metadata = ObjectMeta(name = servable.serviceName))
        .withSelector(CloudDriver.Labels.ServiceName -> name)
        .exposeOnPort(skuber.Service.Port("grpc", Protocol.TCP, DefaultConstants.DEFAULT_APP_PORT))
        .addLabels(Map(
          CloudDriver.Labels.ServiceName -> name,
          CloudDriver.Labels.ModelVersionId -> servable.modelVersionId.toString
        ))
      for {
        deployed <- services
        maybeExist = deployed.find(_.metadata.labels.getOrElse(CloudDriver.Labels.ServiceName, "") == name)
        svc <- maybeExist match {
          case Some(value) => Async[F].pure(value)
          case None => AsyncUtil.futureAsync(underlying.create(service))
        }
      } yield svc
    }

    override def removeDeployment(name: String): F[Unit] = for {
      deployed <- deployments
      maybeDeployment = deployed.find(_.metadata.labels.getOrElse(CloudDriver.Labels.ServiceName, "") == name)
      _ <- maybeDeployment match {
        case Some(value) => AsyncUtil.futureAsync(underlying.delete[Deployment](value.metadata.name))
        case None => Async[F].delay(logger.error(s"kube deployment with name `$name` not found"))
      } 
    } yield Unit

    override def removeService(name: String): F[Unit] = for {
      deployed <- services
      maybeService = deployed.find(_.metadata.labels.getOrElse(CloudDriver.Labels.ServiceName, "") == name)
      _ <- maybeService match {
        case Some(value) => AsyncUtil.futureAsync(underlying.delete[Service](value.metadata.name))
        case None => Async[F].delay(logger.error(s"kube service with name `$name` not found"))
      }
    } yield Unit
  }
  
}
