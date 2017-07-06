package io.hydrosphere.serving.clouddriver;

import java.util.List;

/**
 *
 */
public interface MeshManagementService {

    List<Runtime> runtimeList();

    Runtime getRuntimeByName(String name);

    Runtime getRuntimeById(String id);

    List<RuntimeInstance> getRuntimeInstancesByServiceName(String name);

    RuntimeInstance getRuntimeInstance(String id);
}
