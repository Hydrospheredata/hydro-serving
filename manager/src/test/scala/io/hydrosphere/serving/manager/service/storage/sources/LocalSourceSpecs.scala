//package io.hydrosphere.serving.manager.service.storage.sources
//
//import java.io.FileNotFoundException
//
//import io.hydrosphere.serving.manager.service.source.storages.local.{LocalModelStorage, LocalModelStorageDefinition}
//
//class LocalSourceSpecs extends ModelStorageSpec(new LocalModelStorage(LocalModelStorageDefinition("test", None))) {
//  override def getSourceFile(path: String): String = {
//    val resPath = "/test_models/" + path
//    Option(getClass.getResource(resPath))
//      .map(_.getPath)
//      .getOrElse(throw new FileNotFoundException(s"$resPath not found in resources"))
//  }
//}
