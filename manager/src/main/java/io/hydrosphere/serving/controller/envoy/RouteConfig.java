package io.hydrosphere.serving.controller.envoy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 *
 */
@Data
@ToString
public class RouteConfig {
    @JsonProperty("virtual_hosts")
    private List<RouteHostTO> virtualHosts;
}
