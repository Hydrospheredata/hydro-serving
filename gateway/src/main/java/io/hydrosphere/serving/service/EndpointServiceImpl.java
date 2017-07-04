package io.hydrosphere.serving.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hydrosphere.serving.config.SideCarConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
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

    public EndpointDefinition endpointDefinition(String endpoint) {
        return definitions.get(endpoint);
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
