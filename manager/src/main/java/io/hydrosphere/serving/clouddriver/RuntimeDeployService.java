package io.hydrosphere.serving.clouddriver;

import java.util.List;

/**
 *
 */
public interface RuntimeDeployService {

    void scale(String runtimeName, int scale);

    String deploy(Runtime runtime);

    Runtime getRuntime(String runtimeName);

    List<RuntimeInstance> runtimeInstances();

    List<Runtime> runtimeList();

    List<RuntimeInstance> runtimeInstances(String runtimeName);
}
