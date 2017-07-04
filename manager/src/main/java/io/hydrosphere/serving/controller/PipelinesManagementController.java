package io.hydrosphere.serving.controller;

import io.hydrosphere.serving.service.EndpointDefinition;
import io.hydrosphere.serving.service.EndpointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO move to manager module
 */
@RestController
@RequestMapping("/api/v1/pipelines")
public class PipelinesManagementController {

    @Autowired
    EndpointService endpointService;

    @RequestMapping(method = RequestMethod.PUT)
    public void create(@RequestBody PipelineTO pipeline) {
        endpointService.create(map(pipeline));
    }

    @RequestMapping(method = RequestMethod.POST)
    public void update(@RequestBody PipelineTO pipeline) {
        endpointService.updateDefinition(map(pipeline));
    }

    @RequestMapping(path = "/{pipelineName}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String pipelineName) {
        endpointService.delete(pipelineName);
    }

    @RequestMapping(path = "/{pipelineName}", method = RequestMethod.GET)
    public void get(@PathVariable String pipelineName) {
        endpointService.getDefinition(pipelineName);
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<PipelineTO> list() {
        List<PipelineTO> tos = new ArrayList<>();
        endpointService.definitions().forEach(p -> tos.add(map(p)));
        return tos;
    }

    private PipelineTO map(EndpointDefinition definition) {
        PipelineTO to = new PipelineTO();
        to.setName(definition.getName());
        to.setTransportType(definition.getTransportType());
        to.setChain(new ArrayList<>(definition.getChain()));
        return to;
    }

    private EndpointDefinition map(PipelineTO to) {
        EndpointDefinition definition = new EndpointDefinition();
        definition.setName(to.getName());
        definition.setTransportType(to.getTransportType());
        definition.setChain(new ArrayList<>(to.getChain()));
        return definition;
    }
}
