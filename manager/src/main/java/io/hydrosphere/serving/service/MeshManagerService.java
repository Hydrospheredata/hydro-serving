package io.hydrosphere.serving.service;

import io.hydrosphere.serving.controller.envoy.ClusterConfigTO;
import io.hydrosphere.serving.controller.envoy.RouteConfig;
import io.hydrosphere.serving.controller.envoy.ServiceConfigTO;

import java.util.List;

/**
 *
 */
public interface MeshManagerService {
    RouteConfig routes(String configName, ServiceType cluster, String node);

    ClusterConfigTO clusters(ServiceType cluster, String node);

    ServiceConfigTO services(String serviceName);

    List<Service> getRuntimes();
}
