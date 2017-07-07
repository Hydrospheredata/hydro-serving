package io.hydrosphere.serving.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 */
public class PipelineServiceImpl implements PipelineService {
    private Map<String, Pipeline> pipelines = new ConcurrentHashMap<>();

    @Override
    public void create(Pipeline pipeline) {
        pipelines.put(pipeline.getName(), pipeline);
    }

    @Override
    public Optional<Pipeline> getPipeline(String name) {
        return Optional.ofNullable(pipelines.get(name));
    }

    @Override
    public void delete(String name) {
        pipelines.remove(name);
    }

    @Override
    public void updatePipeline(Pipeline pipeline) {
        pipelines.put(pipeline.getName(), pipeline);
    }

    @Override
    public List<Pipeline> pipelines() {
        return pipelines.values().stream()
                .sorted(Comparator.comparing(Pipeline::getName))
                .collect(Collectors.toList());
    }
}
