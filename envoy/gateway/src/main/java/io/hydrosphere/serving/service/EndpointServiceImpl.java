package io.hydrosphere.serving.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.hydrosphere.serving.config.SideCarConfig;
import io.hydrosphere.serving.proto.ServingPipeline;
import io.hydrosphere.serving.proto.Stage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties;

    public EndpointServiceImpl(SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties) {
        this.sideCarConfigurationProperties = sideCarConfigurationProperties;
    }

    public EndpointDefinition endpointDefinition(String endpoint){
        return definitions.get(endpoint);
    }

    @Override
    public ServingPipeline getPipeline(String endpoint, JsonNode jsonNode) {
        EndpointDefinition definition = definitions.get(endpoint);
        if (definition == null) {
            return null;
        }
        List<Stage> stages = new ArrayList<>();
        definition.getChain().forEach(c -> {
            stages.add(Stage.newBuilder()
                    .setAction("action")
                    .setDestination(c)
                    .setType(Stage.StageType.SERVE)
                    .build());
        });

        ServingPipeline servingPipeline;
        try {
            servingPipeline = ServingPipeline.newBuilder()
                    .setStartTime(System.currentTimeMillis())
                    .setRequestId(UUID.randomUUID().toString())//TODO BAD UUID generator
                    .setGatewayDestination(this.sideCarConfigurationProperties.getServiceId())
                    .addAllStages(stages)
                    .setData(ByteString.copyFrom(objectMapper.writeValueAsBytes(jsonNode)))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return servingPipeline;
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedDelay = 3000L)
    public void syncEndpoint() {
        ResponseEntity<List<EndpointDefinition>> responseEntity = restTemplate.exchange("http://" + sideCarConfigurationProperties.getManagerHost() + ":" + sideCarConfigurationProperties.getManagerPort()
                + "/api/v1/pipelines", HttpMethod.GET, null, new ParameterizedTypeReference<List<EndpointDefinition>>() {
        });

        responseEntity.getBody().forEach(p -> definitions.put(p.getName(), p));
    }
}
