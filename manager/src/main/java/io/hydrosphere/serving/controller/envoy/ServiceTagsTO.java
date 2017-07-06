package io.hydrosphere.serving.controller.envoy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 *
 */
@Data
public class ServiceTagsTO {
    private String az;

    private String canary;

    @JsonProperty("load_balancing_weight")
    private String loadBalancingWeight;
}
