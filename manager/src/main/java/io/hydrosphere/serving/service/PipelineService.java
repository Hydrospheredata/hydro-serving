package io.hydrosphere.serving.service;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface PipelineService {

    void create(Pipeline pipeline);

    Optional<Pipeline> getPipeline(String name);

    void delete(String name);

    void updatePipeline(Pipeline pipeline);

    List<Pipeline> pipelines();
}
