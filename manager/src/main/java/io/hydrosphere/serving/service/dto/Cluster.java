package io.hydrosphere.serving.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *
 */
@Data
@Builder
public class Cluster {
    private String name;

    private String type;

    @JsonProperty("connect_timeout_ms")
    private long connectTimeoutMs;

    @JsonProperty("lb_type")
    private String lbType;

    private List<ClusterHost> hosts;

    @JsonProperty("service_name")
    private String serviceName;

    private String features;
}
