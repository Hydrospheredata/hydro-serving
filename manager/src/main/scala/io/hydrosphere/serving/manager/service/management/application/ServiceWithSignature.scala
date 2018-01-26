package io.hydrosphere.serving.manager.service.management.application

import io.hydrosphere.serving.manager.model.Service

case class ServiceWithSignature(s: Service, signatureName: String)

