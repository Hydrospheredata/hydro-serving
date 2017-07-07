package io.hydrosphere.serving.service.runtime;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface MeshManagementService {

    List<Runtime> runtimeList();

    Optional<Runtime> getRuntimeByName(String name);

    Optional<Runtime> getRuntimeById(String id);

    List<RuntimeInstance> getRuntimeInstancesByServiceName(String name);

    Optional<RuntimeInstance> getRuntimeInstance(String id);
}
