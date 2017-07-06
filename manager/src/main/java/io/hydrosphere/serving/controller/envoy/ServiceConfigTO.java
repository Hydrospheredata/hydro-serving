package io.hydrosphere.serving.controller.envoy;

import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
public class ServiceConfigTO {
    private List<ServiceHostTO> hosts;
}
