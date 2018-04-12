package io.hydrosphere.serving.manager.service.application

import io.hydrosphere.serving.manager.model.db.Service

case class ServiceWithSignature(s: Service, signatureName: String)
