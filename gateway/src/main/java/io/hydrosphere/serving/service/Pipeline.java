package io.hydrosphere.serving.service;

import lombok.Data;

import java.util.List;

/**
 * TODO use common class with manager or use swagger TO's
 */
@Data
public class Pipeline {

    private String name;

    /**
     * {runtimeName}/some/path
     */
    private List<String> invocationChain;
}
