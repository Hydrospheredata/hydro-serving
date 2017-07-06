package io.hydrosphere.serving.clouddriver;

import lombok.Data;

import java.util.Map;

/**
 *
 */
@Data
public class Runtime {

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
}
