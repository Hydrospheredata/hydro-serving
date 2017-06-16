package io.hydrosphere.serving.controller;

import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
public class PipelineTO {
    private String name;

    private List<String> chain;

    private String transportType;
}
