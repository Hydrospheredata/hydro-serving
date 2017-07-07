package io.hydrosphere.serving.service;

import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
public class Pipeline {

    private String name;

    /**
     * {runtimeName}/some/path
     */
    private List<String> invocationChain;
}
