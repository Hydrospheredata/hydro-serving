package io.hydrosphere.serving.service.runtime;

import lombok.Data;

/**
 *
 */
@Data
public class RuntimeInstance {

    private String id;

    private String runtimeId;

    private String host;

    private int httpPort=8080;

    private RuntimeInstanceStatus status;

    private String statusText;

    public enum RuntimeInstanceStatus{
        UP,
        DOWN
    }
}
