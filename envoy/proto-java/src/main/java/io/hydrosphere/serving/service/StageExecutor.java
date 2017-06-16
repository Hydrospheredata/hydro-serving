package io.hydrosphere.serving.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
public interface StageExecutor {

    JsonNode execute(String action, JsonNode data);
}
