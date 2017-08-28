package io.hydrosphere.serving.manager.service.modelbuild

import java.util.Collections

import com.amazonaws.services.ecr.{AmazonECR, AmazonECRClientBuilder}
import com.amazonaws.services.ecr.model._
import com.amazonaws.services.identitymanagement.{AmazonIdentityManagement, AmazonIdentityManagementClientBuilder}
import com.spotify.docker.client.DockerClient
import io.hydrosphere.serving.manager.ECSDockerRepositoryConfiguration
import io.hydrosphere.serving.manager.model.{ModelBuild, ModelRuntime}

/**
  *
  */
class ECSModelPushService(
  dockerClient: DockerClient,
  ecsDockerRepositoryConfiguration: ECSDockerRepositoryConfiguration
) extends ModelPushService {

  val iamClient: AmazonIdentityManagement = AmazonIdentityManagementClientBuilder.standard()
    .withRegion(ecsDockerRepositoryConfiguration.region)
    .build()

  val ecrClient: AmazonECR = AmazonECRClientBuilder.standard()
    .withRegion(ecsDockerRepositoryConfiguration.region)
    .build()

  val accountId: String = iamClient.getUser()
    .getUser.getArn.split(':')(4)

  private def getDockerRegistryAuth: DockerRegistryAuth = {
    val getAuthorizationTokenRequest = new GetAuthorizationTokenRequest
    getAuthorizationTokenRequest.setRegistryIds(Collections.singletonList(accountId))
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
    req.setRegistryId(accountId)
    try {
      ecrClient.describeRepositories(req)
    } catch {
      case ex: RepositoryNotFoundException =>
        val createRepositoryRequest = new CreateRepositoryRequest
        createRepositoryRequest.setRepositoryName(modelName)
        ecrClient.createRepository(createRepositoryRequest)
    }
  }


  override def getImageName(modelBuild: ModelBuild): String = {
    s"$accountId.dkr.ecr.${ecsDockerRepositoryConfiguration.region.getName}.amazonaws.com/${modelBuild.model.name}"
  }

  override def push(modelRuntime: ModelRuntime, progressHandler: ProgressHandler): Unit = {
    createRepositoryIfNeeded(modelRuntime.modelName)

    dockerClient.push(
      s"${modelRuntime.imageName}:${modelRuntime.modelVersion}",
      DockerClientHelper.createProgressHadlerWrapper(progressHandler),
      DockerClientHelper.createRegistryAuth(getDockerRegistryAuth)
    )
  }
}
