package io.hydrosphere.serving.service;

import java.util.Optional;

/**
 *
 */
public interface GatewayPipelineService {
    Optional<Pipeline> pipeline(String name);
}
