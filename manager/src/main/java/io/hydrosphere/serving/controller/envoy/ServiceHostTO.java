package io.hydrosphere.serving.controller.envoy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 *
 */
@Data
public class ServiceHostTO {
    @JsonProperty("ip_address")
    private String ipAddress;

    private int port;

    private ServiceTagsTO tags;
    /*
    "ip_address": "...",
  "port": "...",
  "tags": {
    "az": "...",
    "canary": "...",
    "load_balancing_weight": "..."
  }
    */
}
