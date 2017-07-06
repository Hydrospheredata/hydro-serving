package io.hydrosphere.serving.controller.envoy;

import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
public class RouteHostTO {
    private String name;

    private List<String> domains;

    private List<RouteTO> routes;
}
