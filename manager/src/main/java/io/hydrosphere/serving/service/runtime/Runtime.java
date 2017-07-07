package io.hydrosphere.serving.service.runtime;

import lombok.Data;

import java.util.Map;

/**
 *
 */
@Data
public class Runtime {
    private String id;

    private String name;

    private int scale;

    private String runtimeType;

    private String modelName;

    private String modelVersion;

    private Map<String, String> environments;

    private String state;

    private String statusText;

    private String imageName;

    private int httpPort=8080;

    private int appHttpPort=9090;
}
