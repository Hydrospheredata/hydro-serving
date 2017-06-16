package io.hydrosphere.serving.service;

import lombok.Data;

import java.util.List;

/**
 * TODO use class from manager
 */
@Data
public class EndpointDefinition {
    private String name;

    private String transportType;

    private List<String> chain;
}
