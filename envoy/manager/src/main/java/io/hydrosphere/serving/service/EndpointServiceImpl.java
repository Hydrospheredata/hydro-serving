package io.hydrosphere.serving.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.hydrosphere.serving.config.SideCarConfig;
import io.hydrosphere.serving.proto.ServingPipeline;
import io.hydrosphere.serving.proto.Stage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Service
public class EndpointServiceImpl implements EndpointService {
    private Map<String, EndpointDefinition> definitions = new ConcurrentHashMap<>();

    private final SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties;

    public EndpointServiceImpl(SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties) {
        this.sideCarConfigurationProperties = sideCarConfigurationProperties;
    }

    @Override
    public void create(EndpointDefinition definition) {
        definitions.put(definition.getName(), definition);
    }

    @Override
    public EndpointDefinition getDefinition(String name) {
        return definitions.get(name);
    }

    @Override
    public void delete(String name) {
        definitions.remove(name);
    }

    @Override
    public void updateDefinition(EndpointDefinition definition) {
        definitions.put(definition.getName(), definition);
    }

    @Override
    public List<EndpointDefinition> definitions() {
        return new ArrayList<>(definitions.values());
    }
}
