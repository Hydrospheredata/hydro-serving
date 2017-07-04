package io.hydrosphere.serving.service;

import java.util.List;

/**
 *
 */
public interface EndpointService {
    void create(EndpointDefinition definition);

    EndpointDefinition getDefinition(String name);

    void delete(String name);

    void updateDefinition(EndpointDefinition definition);

    List<EndpointDefinition> definitions();
}
