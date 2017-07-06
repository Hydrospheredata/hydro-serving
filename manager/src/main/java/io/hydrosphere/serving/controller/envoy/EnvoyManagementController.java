package io.hydrosphere.serving.controller.envoy;

import io.hydrosphere.serving.clouddriver.MeshManagementService;
import io.hydrosphere.serving.clouddriver.Runtime;
import io.hydrosphere.serving.clouddriver.RuntimeInstance;
import io.hydrosphere.serving.config.ManagerConfig;
import io.hydrosphere.serving.service.MeshManagerService;
import io.hydrosphere.serving.service.ServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 *
 */
@RestController
@RequestMapping("/v1")
public class EnvoyManagementController {

    @Autowired
    private MeshManagerService managerService;

    @Autowired
    private MeshManagementService meshManagementService;

    @Autowired
    private ManagerConfig.ManagerConfigurationProperties configurationProperties;

    //TODO reduce clusters based on pipelines configuration
    @RequestMapping(value = "/clusters/{cluster}/{node}", method = RequestMethod.GET)
    public ClusterConfigTO clusters(@PathVariable ServiceType cluster,
                                    @PathVariable String node) {
        ClusterConfigTO config = new ClusterConfigTO();
        List<ClusterTO> clusters = managerClusters(node);
        config.setClusters(clusters);
        return config;
    }

    private List<ClusterTO> managerClusters(String node) {
        RuntimeInstance runtimeInstance = meshManagementService.getRuntimeInstance(node);
        if (runtimeInstance == null) {
            return Collections.emptyList();
        }
        Runtime runtime = meshManagementService.getRuntimeById(runtimeInstance.getRuntimeId());
        if (runtime == null) {
            return Collections.emptyList();
        }

        List<ClusterTO> result = new ArrayList<>();
        ClusterTO.ClusterTOBuilder cluster = ClusterTO.builder()
                .features(null)
                .connectTimeoutMs(500)
                .lbType("round_robin");
        meshManagementService.runtimeList().forEach(r -> {
            cluster.serviceName(r.getName())
                    .name(r.getName());
            if (r.getName().equals(runtime.getName())) {
                cluster.hosts(Collections.singletonList(new ClusterHostTO("tcp://127.0.0.1:" + runtime.getAppHttpPort())))
                        .type("static");
            } else {
                cluster.hosts(null)
                        .type("sds");
            }
            result.add(cluster.build());
        });


        Runtime gatewayRuntime = meshManagementService.getRuntimeByName(configurationProperties.getGatewayServiceName());
        meshManagementService.getRuntimeInstancesByServiceName(configurationProperties.getGatewayServiceName())
                .forEach(p -> {
                    result.add(cluster.serviceName(null)
                            //TODO change in Envoy maximum name length to 64
                            .name(UUID.nameUUIDFromBytes(p.getId().getBytes()).toString())
                            .type("static")
                            .hosts(getStaticHost(gatewayRuntime, p, node))
                            .build());
                });
        return result;
    }

    private List<ClusterHostTO> getStaticHost(Runtime runtime, RuntimeInstance service, String forNode) {
        boolean sameNode = service.getId().equals(forNode);
        StringBuilder builder = new StringBuilder("tcp://");
        if (sameNode) {
            builder.append("127.0.0.1");
        } else {
            builder.append(service.getHost());
        }

        builder.append(":");
        if (sameNode) {
            builder.append(runtime.getAppHttpPort());
        } else {
            builder.append(service.getHttpPort());
        }
        return Collections.singletonList(new ClusterHostTO(builder.toString()));
    }

    //TODO reduce routes based on pipelines configuration
    @RequestMapping(value = "/routes/{configName}/{cluster}/{node}", method = RequestMethod.GET)
    public RouteConfig routes(@PathVariable String configName,
                              @PathVariable ServiceType cluster,
                              @PathVariable String node) {
        List<RouteHostTO> routeHosts = new ArrayList<>();
        meshManagementService.runtimeList().forEach((service) -> {
            RouteHostTO routeHost = new RouteHostTO();
            routeHost.setDomains(Collections.singletonList(service.getName()));
            routeHost.setName(service.getName());
            routeHost.setRoutes(Collections.singletonList(new RouteTO("/", service.getName())));
            routeHosts.add(routeHost);
        });
        RouteConfig routeConfig = new RouteConfig();
        routeConfig.setVirtualHosts(routeHosts);
        return managerService.routes(configName, cluster, node);
    }

    @RequestMapping(value = "/registration/{serviceName}", method = RequestMethod.GET)
    public ServiceConfigTO services(@PathVariable String serviceName) {
        List<ServiceHostTO> hosts = new ArrayList<>();
        meshManagementService.getRuntimeInstancesByServiceName(serviceName)
                .forEach(p -> {
                    ServiceHostTO host = new ServiceHostTO();
                    host.setPort(p.getHttpPort());
                    host.setIpAddress(p.getHost());
                    hosts.add(host);
                });
        ServiceConfigTO config = new ServiceConfigTO();
        config.setHosts(hosts);
        return config;
    }
}
