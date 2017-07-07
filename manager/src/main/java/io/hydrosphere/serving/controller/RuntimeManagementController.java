package io.hydrosphere.serving.controller;

import io.hydrosphere.serving.service.runtime.MeshManagementService;
import io.hydrosphere.serving.service.runtime.Runtime;
import io.hydrosphere.serving.service.runtime.RuntimeDeployService;
import io.hydrosphere.serving.service.runtime.RuntimeInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 */
@RestController
@RequestMapping("/api/v1/runtime")
public class RuntimeManagementController {

    @Autowired
    private MeshManagementService meshManagementService;

    @Autowired
    private RuntimeDeployService runtimeDeployService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public void create(Runtime runtime) {
        runtimeDeployService.deploy(runtime);
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<Runtime> list() {
        return meshManagementService.runtimeList();
    }

    @RequestMapping(path = "/{runtimeName}", method = RequestMethod.GET)
    public Runtime get(@PathVariable String runtimeName) {
        return meshManagementService.getRuntimeByName(runtimeName)
                .orElseThrow(IllegalArgumentException::new);
    }

    @RequestMapping(path = "/instances/{runtimeName}", method = RequestMethod.GET)
    public List<RuntimeInstance> instances(@PathVariable String runtimeName) {
        return meshManagementService.getRuntimeInstancesByServiceName(runtimeName);
    }

    @RequestMapping(path = "/{runtimeName}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String runtimeName) {
        meshManagementService.getRuntimeByName(runtimeName)
                .ifPresent(p -> runtimeDeployService.deleteRuntime(p.getId()));
    }
}
