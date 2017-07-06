package io.hydrosphere.serving.clouddriver;

import java.lang.*;
import java.util.List;

/**
 *
 */
public interface RuntimeDeployService {

    void scale(String runtimeName, int scale);

    String deploy(java.lang.Runtime runtime);

    java.lang.Runtime getRuntime(String runtimeName);

    List<java.lang.Runtime> runtimeList();

    List<ServiceInstance> runtimeInstances(String runtimeName);
}
