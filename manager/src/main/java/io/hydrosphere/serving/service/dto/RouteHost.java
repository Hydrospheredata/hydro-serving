package io.hydrosphere.serving.service.dto;

import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
public class RouteHost {
    private String name;

    private List<String> domains;

    private List<Route> routes;
}
