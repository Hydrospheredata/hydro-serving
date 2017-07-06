package io.hydrosphere.serving.controller.envoy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
@Builder
public class ClusterTO {
    private String name;

    private String type;

    @JsonProperty("connect_timeout_ms")
    private long connectTimeoutMs;

    @JsonProperty("lb_type")
    private String lbType;

    private List<ClusterHostTO> hosts;

    @JsonProperty("service_name")
    private String serviceName;

    private String features;
}
