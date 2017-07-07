package io.hydrosphere.serving.controller;

import io.hydrosphere.serving.service.Pipeline;
import io.hydrosphere.serving.service.PipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 */
@RestController
@RequestMapping("/api/v1/pipelines")
public class PipelinesManagementController {

    @Autowired
    private PipelineService pipelineService;

    @RequestMapping(method = RequestMethod.POST)
    public void create(@RequestBody Pipeline pipeline) {
        pipelineService.create(pipeline);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public void update(@RequestBody Pipeline pipeline) {
        pipelineService.updatePipeline(pipeline);
    }

    @RequestMapping(path = "/{pipelineName}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String pipelineName) {
        pipelineService.delete(pipelineName);
    }

    @RequestMapping(path = "/{pipelineName}", method = RequestMethod.GET)
    public Pipeline get(@PathVariable String pipelineName) {
        return pipelineService.getPipeline(pipelineName).orElseThrow(IllegalArgumentException::new);
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<Pipeline> list() {
        return pipelineService.pipelines();
    }
}
