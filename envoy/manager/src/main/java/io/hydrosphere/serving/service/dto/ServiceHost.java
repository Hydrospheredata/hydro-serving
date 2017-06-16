package io.hydrosphere.serving.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 *
 */
@Data
public class ServiceHost {
    @JsonProperty("ip_address")
    private String ipAddress;

    private int port;

    private ServiceTags tags;
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
