package io.hydrosphere.serving.manager.infrastructure.model.push

import java.util.Collections

import com.amazonaws.services.ecr.{AmazonECR, AmazonECRClientBuilder}
import com.amazonaws.services.ecr.model._
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.manager.config.DockerRepositoryConfiguration
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionPushAlgebra}
import io.hydrosphere.serving.manager.util.docker.{DockerClientHelper, DockerRegistryAuth}

class ECSModelPushService(
  dockerClient: DockerClient,
  ecsDockerRepositoryConfiguration: DockerRepositoryConfiguration.Ecs
) extends ModelVersionPushAlgebra {

  val ecrClient: AmazonECR = AmazonECRClientBuilder.standard()
    .withRegion(ecsDockerRepositoryConfiguration.region)
    .build()

  private def getDockerRegistryAuth: DockerRegistryAuth = {
    val getAuthorizationTokenRequest = new GetAuthorizationTokenRequest
    getAuthorizationTokenRequest.setRegistryIds(Collections.singletonList(ecsDockerRepositoryConfiguration.accountId))
    val result = ecrClient.getAuthorizationToken(getAuthorizationTokenRequest)

    val authorizationData = result.getAuthorizationData.get(0)

    DockerRegistryAuth(
      username = None,
      password = None,
      email = None,
      identityToken = None,
      serverAddress = Some(authorizationData.getProxyEndpoint),
      auth = Some(authorizationData.getAuthorizationToken)
    )
  }

  private def createRepositoryIfNeeded(modelName: String): Unit = {
    val req = new DescribeRepositoriesRequest
    req.setRepositoryNames(Collections.singletonList(modelName))
    req.setRegistryId(ecsDockerRepositoryConfiguration.accountId)
    try {
      ecrClient.describeRepositories(req)
    } catch {
      case _: RepositoryNotFoundException =>
        val createRepositoryRequest = new CreateRepositoryRequest
        createRepositoryRequest.setRepositoryName(modelName)
        ecrClient.createRepository(createRepositoryRequest)
    }
  }


  override def getImage(modelName: String, modelVersion: Long): DockerImage = {
    DockerImage(
      name = s"${ecsDockerRepositoryConfiguration.accountId}.dkr.ecr.${ecsDockerRepositoryConfiguration.region.getName}.amazonaws.com/$modelName",
      tag = modelVersion.toString
    )
  }

  override def push(modelVersion: ModelVersion, progressHandler: ProgressHandler): Unit = {
    createRepositoryIfNeeded(modelVersion.model.name)
    dockerClient.push(
      s"${modelVersion.image.name}:${modelVersion.modelVersion}",
      progressHandler,
      DockerClientHelper.createRegistryAuth(getDockerRegistryAuth)
    )
  }
}