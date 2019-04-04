package io.hydrosphere.serving.manager.domain.clouddriver

import cats.implicits._
import cats.effect.Sync

import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.spotify.docker.client.DockerClient.{ListContainersParam, RemoveContainerParam}
import com.spotify.docker.client.messages.{Container, ContainerConfig, ContainerCreation}

import scala.collection.JavaConverters._

trait DockerdClient[F[_]]{
  
  def createContainer(container: ContainerConfig, name: Option[String]): F[ContainerCreation]
  
  def runContainer(id: String): F[Unit]
  
  def removeContainer(id: String, params: List[RemoveContainerParam]): F[Unit]
  def removeContainer(id: String): F[Unit] = removeContainer(id, Nil)
  
  def listContainers(params: List[ListContainersParam]): F[List[Container]]
  def listContainers: F[List[Container]] = listContainers(Nil)
}

object DockerdClient {
  
  def create[F[_]](implicit F: Sync[F]): DockerdClient[F] =
    DockerdClient.create(DefaultDockerClient.fromEnv().build())
  
  def create[F[_]](underlying: DockerClient)(implicit F: Sync[F]): DockerdClient[F] =
    new DockerdClient[F] {
      
      override def createContainer(container: ContainerConfig, name: Option[String]): F[ContainerCreation] = {
        F.delay {
          name match {
            case Some(n) => underlying.createContainer(container, n)
            case None => underlying.createContainer(container)
          }
        }
      }
      
      override def runContainer(id: String): F[Unit] =
        F.delay(underlying.startContainer(id))
  
      override def removeContainer(id: String, params: List[RemoveContainerParam]): F[Unit] = {
        F.delay(underlying.removeContainer(id, params: _*))
      }
  
      override def listContainers(params: List[ListContainersParam]): F[List[Container]] = {
        F.delay(underlying.listContainers(params: _*)).map(_.asScala.toList)
      }
      
    }
  
}

