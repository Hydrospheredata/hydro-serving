package io.hydrosphere.serving.service;

import io.hydrosphere.serving.service.dto.ClusterConfig;
import io.hydrosphere.serving.service.dto.RouteConfig;
import io.hydrosphere.serving.service.dto.ServiceConfig;

import java.util.List;

/**
 *
 */
public interface MeshManagerService {
    RouteConfig routes(String configName, ServiceType cluster, String node);

    ClusterConfig clusters(ServiceType cluster, String node);

    ServiceConfig services(String serviceName);

    void unregisterService(String serviceId);

    void registerService(Service service);

    Service getService(String serviceId);

    List<Service> getRuntimes();
}
