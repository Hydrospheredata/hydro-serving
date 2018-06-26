package io.hydrosphere.serving.manager.service.model

import io.hydrosphere.serving.manager.controller.model.{ModelUpload, UploadedEntity}
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.description.ContractDescription
import io.hydrosphere.serving.manager.model.db.Model
import spray.json.JsObject

import scala.concurrent.Future

trait ModelManagementService {

  /***
    * Uploads the tarball to source and creates a model entry in manager.
    * @param upload tarball with metadata
    * @return uploaded model
    */
  def uploadModel(upload: ModelUpload): HFResult[Model]

  /***
    * Get flat contract description
    * @param modelId model id
    * @return contract description
    */
  def modelContractDescription(modelId: Long): HFResult[ContractDescription]

  /***
    * Submit contract in binary encoding
    * @param modelId model id
    * @param bytes contract
    * @return updated model
    */
  def submitBinaryContract(modelId: Long, bytes: Array[Byte]): HFResult[Model]

  /***
    * Submit contract in flat encoding
    * @param modelId model id
    * @param contractDescription contract
    * @return updated model
    */
  def submitFlatContract(modelId: Long, contractDescription: ContractDescription): HFResult[Model]

  /***
    * Submit contract in ASCII encoding
    * @param modelId model id
    * @param prototext contract
    * @return updated model
    */
  def submitContract(modelId: Long, prototext: String): HFResult[Model]

  /***
    * Get all models
    * @return
    */
  def allModels(): Future[Seq[Model]]

  /***
    * Try to get model by id
    * @param id
    * @return
    */
  def getModel(id: Long): HFResult[Model]

  /***
    * Try to update a model by request
    * @param entity
    * @return
    */
  def updateModel(entity: UpdateModelRequest): HFResult[Model]

  /***
    * Try to create a model by request
    * @param entity
    * @return
    */
  def createModel(entity: CreateModelRequest): HFResult[Model]

  /***
    * Try to generate an example input for a model
    * @param modelId
    * @param signature
    * @return
    */
  def generateModelPayload(modelId: Long, signature: String): HFResult[JsObject]

  /***
    * Get all models with specified ModelType
    * @param types
    * @return
    */
  def modelsByType(types: Set[String]): Future[Seq[Model]]

  /**
    * Deletes a model if possible. Fails if there are dependencies for the model.
    * @param modelId
    * @return
    */
  def delete(modelId: Long): HFResult[Model]
}
