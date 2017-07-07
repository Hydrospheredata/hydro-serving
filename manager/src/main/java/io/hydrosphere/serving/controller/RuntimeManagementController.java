package io.hydrosphere.serving.controller;

import io.hydrosphere.serving.config.ManagerConfig;
import io.hydrosphere.serving.service.runtime.MeshManagementService;
import io.hydrosphere.serving.service.runtime.Runtime;
import io.hydrosphere.serving.service.runtime.RuntimeDeployService;
import io.hydrosphere.serving.service.runtime.RuntimeInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Controller
@RequestMapping("/api/v1/runtime")
public class RuntimeManagementController {

    @Autowired
    private MeshManagementService meshManagementService;

    @Autowired
    private RuntimeDeployService runtimeDeployService;

    @Autowired
    private ManagerConfig.ManagerConfigurationProperties configurationProperties;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody Runtime runtime) {

        //TODO move to service
        Map<String, String> env;
        if (runtime.getEnvironments() == null) {
            env = new HashMap<>();
            runtime.setEnvironments(env);
        } else {
            env = runtime.getEnvironments();
        }
        env.put("MANAGER_HOST", configurationProperties.getExposedHost());
        env.put("MANAGER_PORT", String.valueOf(configurationProperties.getExposedPort()));
        runtimeDeployService.deploy(runtime);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<Runtime> list() {
        return meshManagementService.runtimeList();
    }

    @RequestMapping(path = "/{runtimeName}", method = RequestMethod.GET)
    @ResponseBody
    public Runtime get(@PathVariable String runtimeName) {
        return meshManagementService.getRuntimeByName(runtimeName)
                .orElseThrow(IllegalArgumentException::new);
    }

    @RequestMapping(path = "/instances/{runtimeName}", method = RequestMethod.GET)
    @ResponseBody
    public List<RuntimeInstance> instances(@PathVariable String runtimeName) {
        return meshManagementService.getRuntimeInstancesByServiceName(runtimeName);
    }

    @RequestMapping(path = "/{runtimeName}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String runtimeName) {
        meshManagementService.getRuntimeByName(runtimeName)
                .ifPresent(p -> runtimeDeployService.deleteRuntime(p.getId()));
    }
}
