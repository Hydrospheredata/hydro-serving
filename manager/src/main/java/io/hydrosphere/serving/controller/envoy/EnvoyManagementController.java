package io.hydrosphere.serving.controller.envoy;

import io.hydrosphere.serving.config.ManagerConfig;
import io.hydrosphere.serving.service.runtime.MeshManagementService;
import io.hydrosphere.serving.service.runtime.Runtime;
import io.hydrosphere.serving.service.runtime.RuntimeInstance;
import io.hydrosphere.serving.service.runtime.RuntimeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 *
 */
@Controller
@RequestMapping("/v1")
public class EnvoyManagementController {

    private static final Logger LOGGER = LogManager.getLogger();

    @Autowired
    private MeshManagementService meshManagementService;

    @Autowired
    private ManagerConfig.ManagerConfigurationProperties configurationProperties;

    //TODO reduce clusters based on pipelines configuration
    @RequestMapping(value = "/clusters/{cluster}/{node}", method = RequestMethod.GET)
    @ResponseBody
    public ClusterConfigTO clusters(@PathVariable RuntimeType cluster,
                                    @PathVariable String node) {
        List<ClusterTO> clusters = managerClusters(node);
        LOGGER.trace("clusters: {}", clusters);
        ClusterConfigTO config = new ClusterConfigTO();
        config.setClusters(clusters);
        return config;
    }

    private List<ClusterTO> managerClusters(String node) {
        Optional<RuntimeInstance> runtimeInstance = meshManagementService.getRuntimeInstance(node);
        if (!runtimeInstance.isPresent()) {
            return Collections.emptyList();
        }
        Optional<Runtime> runtime = meshManagementService.getRuntimeById(runtimeInstance.get().getRuntimeId());
        if (!runtime.isPresent()) {
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
            if (r.getName().equals(runtime.get().getName())) {
                cluster.serviceName(null)
                        .hosts(Collections.singletonList(new ClusterHostTO("tcp://127.0.0.1:" + runtime.get().getAppHttpPort())))
                        .type("static");
            } else {
                cluster.hosts(null)
                        .type("sds");
            }
            result.add(cluster.build());
        });

        if (runtime.get().getRuntimeType() == RuntimeType.model) {
            Optional<Runtime> gatewayRuntime = meshManagementService.getRuntimeByName(configurationProperties.getGatewayServiceName());
            gatewayRuntime.ifPresent(r -> meshManagementService.getRuntimeInstancesByServiceName(configurationProperties.getGatewayServiceName())
                    .forEach(p -> result.add(cluster.serviceName(null)
                            //TODO change in Envoy maximum name length to 64
                            .name(UUID.nameUUIDFromBytes(p.getId().getBytes()).toString())
                            .serviceName(null)
                            .type("static")
                            .hosts(getStaticHost(r, p, node))
                            .build())));
        }
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
    @ResponseBody
    public RouteConfig routes(@PathVariable String configName,
                              @PathVariable RuntimeType cluster,
                              @PathVariable String node) {
        Optional<RuntimeInstance> runtimeInstance = meshManagementService.getRuntimeInstance(node);
        if (!runtimeInstance.isPresent()) {
            throw new IllegalArgumentException("Node id");
        }
        Optional<Runtime> runtime = meshManagementService.getRuntimeById(runtimeInstance.get().getRuntimeId());
        if (!runtime.isPresent()) {
            throw new IllegalArgumentException("Service id");
        }

        List<RouteHostTO> routeHosts = new ArrayList<>();
        meshManagementService.runtimeList().stream()
                .filter(s -> !s.getName().equals(runtime.get().getName()))
                .forEach((service) -> {
                    RouteHostTO routeHost = new RouteHostTO();
                    routeHost.setDomains(Collections.singletonList(service.getName()));
                    routeHost.setName(service.getName());
                    routeHost.setRoutes(Collections.singletonList(new RouteTO("/", service.getName())));
                    routeHosts.add(routeHost);
                });

        meshManagementService.getRuntimeInstancesByServiceName(configurationProperties.getGatewayServiceName())
                .forEach((service) -> {
                    RouteHostTO routeHost = new RouteHostTO();
                    routeHost.setDomains(Collections.singletonList(service.getId()));
                    routeHost.setName(service.getId());
                    routeHost.setRoutes(Collections.singletonList(new RouteTO("/",
                            UUID.nameUUIDFromBytes(service.getId().getBytes()).toString())));
                    routeHosts.add(routeHost);
                });

        RouteHostTO routeHost = new RouteHostTO();
        routeHost.setDomains(Collections.singletonList("*"));
        routeHost.setName("all");
        routeHost.setRoutes(Collections.singletonList(new RouteTO("/", runtime.get().getName())));
        routeHosts.add(routeHost);
        LOGGER.trace("routes: {}", routeHosts);
        RouteConfig routeConfig = new RouteConfig();
        routeConfig.setVirtualHosts(routeHosts);
        return routeConfig;
    }

    @RequestMapping(value = "/registration/{serviceName}", method = RequestMethod.GET)
    @ResponseBody
    public ServiceConfigTO services(@PathVariable String serviceName) {
        List<ServiceHostTO> hosts = new ArrayList<>();
        meshManagementService.getRuntimeInstancesByServiceName(serviceName)
                .forEach(p -> {
                    ServiceHostTO host = new ServiceHostTO();
                    host.setPort(p.getHttpPort());
                    host.setIpAddress(p.getHost());
                    hosts.add(host);
                });
        LOGGER.trace("services: {}", hosts);
        ServiceConfigTO config = new ServiceConfigTO();
        config.setHosts(hosts);
        return config;
    }
}
