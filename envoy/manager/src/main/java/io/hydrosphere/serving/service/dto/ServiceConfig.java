package io.hydrosphere.serving.service.dto;

import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
public class ServiceConfig {
    private List<ServiceHost> hosts;
}
