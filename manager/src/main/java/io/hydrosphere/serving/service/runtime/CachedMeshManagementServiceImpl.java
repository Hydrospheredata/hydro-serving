package io.hydrosphere.serving.service.runtime;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;

/**
 *
 */
public class CachedMeshManagementServiceImpl implements MeshManagementService {

    private final RuntimeDeployService runtimeDeployService;

    public CachedMeshManagementServiceImpl(RuntimeDeployService runtimeDeployService) {
        this.runtimeDeployService = runtimeDeployService;
    }

    //TODO Refactor
    private Map<String, Runtime> runtimeMap = new HashMap<>();
    private Map<String, Runtime> runtimeByIdMap = new HashMap<>();
    private Map<String, RuntimeInstance> runtimeInstanceMap = new HashMap<>();
    private Map<String, List<RuntimeInstance>> runtimeInstancesByServiceNameMap = new HashMap<>();

    @Override
    public List<Runtime> runtimeList() {
        return new ArrayList<>(runtimeMap.values());
    }

    @Override
    public Optional<Runtime> getRuntimeByName(String name) {
        return Optional.ofNullable(runtimeMap.get(name));
    }

    @Override
    public Optional<Runtime> getRuntimeById(String id) {
        return Optional.ofNullable(runtimeByIdMap.get(id));
    }

    @Override
    public List<RuntimeInstance> getRuntimeInstancesByServiceName(String name) {
        List<RuntimeInstance> instances = runtimeInstancesByServiceNameMap.get(name);
        return instances == null ? Collections.emptyList() : instances;
    }

    @Override
    public Optional<RuntimeInstance> getRuntimeInstance(String id) {
        return Optional.ofNullable(runtimeInstanceMap.get(id));
    }

    //TODO Refactor
    @Scheduled(fixedDelay = 10000L)
    public void fetch() {
        Map<String, Runtime> map = new HashMap<>();
        Map<String, Runtime> mapById = new HashMap<>();
        runtimeDeployService.runtimeList().forEach(p -> {
            map.put(p.getName(), p);
            mapById.put(p.getId(), p);
        });
        this.runtimeMap = map;
        this.runtimeByIdMap = mapById;


        Map<String, RuntimeInstance> runtimeInstance = new HashMap<>();
        Map<String, List<RuntimeInstance>> runtimeInstancesByServiceName = new HashMap<>();
        runtimeDeployService.runtimeInstances().forEach(p -> {
            Runtime runtime = mapById.get(p.getRuntimeId());
            if (runtime != null) {
                runtimeInstance.put(p.getId(), p);

                List<RuntimeInstance> instances = runtimeInstancesByServiceName.getOrDefault(runtime.getName(), new ArrayList<>());
                instances.add(p);
            }
        });
        this.runtimeInstanceMap = runtimeInstance;
        this.runtimeInstancesByServiceNameMap = runtimeInstancesByServiceName;
    }
}
