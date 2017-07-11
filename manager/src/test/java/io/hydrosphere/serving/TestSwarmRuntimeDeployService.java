package io.hydrosphere.serving;

import com.spotify.docker.client.DefaultDockerClient;
import io.hydrosphere.serving.clouddriver.swarm.SwarmRuntimeDeployService;
import io.hydrosphere.serving.service.runtime.CachedMeshManagementServiceImpl;
import org.junit.Test;
import org.junit.Ignore;

/**
 *
 */
public class TestSwarmRuntimeDeployService {
    @Test
    @Ignore
    public void test() throws Exception{
        SwarmRuntimeDeployService service=new SwarmRuntimeDeployService(DefaultDockerClient.fromEnv().build(), "net1");
        CachedMeshManagementServiceImpl managementService=new CachedMeshManagementServiceImpl(service);
        managementService.fetch();
        service.runtimeInstances();
        service.runtimeList();
    }
}
