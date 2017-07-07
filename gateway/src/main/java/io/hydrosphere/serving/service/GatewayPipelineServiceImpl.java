package io.hydrosphere.serving.service;

import io.hydrosphere.serving.config.GatewayConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
@Service
public class GatewayPipelineServiceImpl implements GatewayPipelineService {

    private Map<String, Pipeline> pipelines = new HashMap<>();

    private final GatewayConfig.GatewayConfigurationProperties configurationProperties;

    public GatewayPipelineServiceImpl(GatewayConfig.GatewayConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedDelay = 3000L)
    public void syncEndpoint() {
        //TODO maybe better to use manager runtime name
        ResponseEntity<List<Pipeline>> responseEntity = restTemplate.exchange("http://" + configurationProperties.getManagerHost() + ":" + configurationProperties.getManagerPort()
                + "/api/v1/pipelines", HttpMethod.GET, null, new ParameterizedTypeReference<List<Pipeline>>() {
        });

        Map<String, Pipeline> map = new HashMap<>();
        responseEntity.getBody().forEach(p -> map.put(p.getName(), p));
        //TODO change
        this.pipelines = map;
    }

    @Override
    public Optional<Pipeline> pipeline(String name) {
        return Optional.ofNullable(pipelines.get(name));
    }
}
