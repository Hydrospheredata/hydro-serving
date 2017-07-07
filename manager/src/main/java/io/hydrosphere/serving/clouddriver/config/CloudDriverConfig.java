package io.hydrosphere.serving.clouddriver.config;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import io.hydrosphere.serving.clouddriver.swarm.SwarmRuntimeDeployService;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class CloudDriverConfig {

    @Configuration
    @ConditionalOnProperty(havingValue = "swarm", name = "clouddriver.type")
    public class SwarmCloudDriverConfig {

        @Bean
        public SwarmRuntimeDeployService swarmRuntimeDeployService() throws DockerCertificateException {
            CloudDriverConfig.SwarmClientConfigurationProperties properties = swarmClientConfigurationProperties();
            return new SwarmRuntimeDeployService(dockerClient(), properties.getNetwork());
        }

        @Bean(destroyMethod = "close")
        public DockerClient dockerClient() throws DockerCertificateException {
            return DefaultDockerClient.fromEnv().build();
        }

        @Bean
        public CloudDriverConfig.SwarmClientConfigurationProperties swarmClientConfigurationProperties() {
            return new CloudDriverConfig.SwarmClientConfigurationProperties();
        }
    }

    @ConfigurationProperties("clouddriver.swarm")
    @Data
    public static class SwarmClientConfigurationProperties {
        private String network;
    }
}
