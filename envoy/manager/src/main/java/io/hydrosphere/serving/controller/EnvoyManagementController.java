package io.hydrosphere.serving.controller;

import io.hydrosphere.serving.service.MeshManagerService;
import io.hydrosphere.serving.service.Service;
import io.hydrosphere.serving.service.ServiceType;
import io.hydrosphere.serving.service.dto.ClusterConfig;
import io.hydrosphere.serving.service.dto.RouteConfig;
import io.hydrosphere.serving.service.dto.ServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 *
 */
@Controller
@RequestMapping("/v1")
public class EnvoyManagementController {

    @Autowired
    private MeshManagerService managerService;

    @RequestMapping(value = "/routes/{configName}/{cluster}/{node}", method = RequestMethod.GET)
    @ResponseBody
    public RouteConfig routes(@PathVariable String configName,
                       @PathVariable ServiceType cluster,
                       @PathVariable String node) {
        return managerService.routes(configName, cluster, node);
    }

    @RequestMapping(value = "/clusters/{cluster}/{node}", method = RequestMethod.GET)
    @ResponseBody
    public ClusterConfig clusters(@PathVariable ServiceType cluster,
                           @PathVariable String node) {
        return managerService.clusters(cluster, node);
    }

    @RequestMapping(value = "/registration/{serviceName}", method = RequestMethod.GET)
    @ResponseBody
    public ServiceConfig services(@PathVariable String serviceName) {
        return managerService.services(serviceName);
    }

    @RequestMapping(value = "/registration", method = RequestMethod.PUT)
    @ResponseBody
    public void register(@RequestBody Service service) {
        managerService.registerService(service);
    }

    @RequestMapping(value = "/registration/{serviceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void unregister(@PathVariable String serviceId) {
        managerService.unregisterService(serviceId);
    }
}
