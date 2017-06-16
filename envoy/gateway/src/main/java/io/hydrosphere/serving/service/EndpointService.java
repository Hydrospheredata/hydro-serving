package io.hydrosphere.serving.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.hydrosphere.serving.proto.ServingPipeline;

import java.util.List;

/**
 *
 */
public interface EndpointService {
    EndpointDefinition endpointDefinition(String endpoint);

    ServingPipeline getPipeline(String endpoint, JsonNode jsonNode);
}
