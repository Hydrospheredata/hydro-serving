package io.hydrosphere.serving.clouddriver;

import lombok.Data;

/**
 *
 */
@Data
public class ServiceInstance {

    private String id;

    private String host;

    private RuntimeInstanceStatus status;

    private String statusText;

    public enum RuntimeInstanceStatus{
        UP,
        DOWN
    }
}
