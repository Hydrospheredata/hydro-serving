package io.hydrosphere.serving.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 *
 */
@Data
public class ServiceTags {
    private String az;

    private String canary;

    @JsonProperty("load_balancing_weight")
    private String loadBalancingWeight;
}
