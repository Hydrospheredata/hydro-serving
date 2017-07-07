package io.hydrosphere.serving.config;

import io.hydrosphere.serving.service.PipelineService;
import io.hydrosphere.serving.service.PipelineServiceImpl;
import io.hydrosphere.serving.service.runtime.CachedMeshManagementServiceImpl;
import io.hydrosphere.serving.service.runtime.MeshManagementService;
import io.hydrosphere.serving.service.runtime.RuntimeDeployService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class ManagerServicesConfig {

    @Bean
    public MeshManagementService meshManagementService(@Autowired RuntimeDeployService runtimeDeployService) {
        return new CachedMeshManagementServiceImpl(runtimeDeployService);
    }

    @Bean
    public PipelineService pipelineService() {
        return new PipelineServiceImpl();
    }
}
