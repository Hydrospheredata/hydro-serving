
package io.hydrosphere.serving.config;

import io.hydrosphere.serving.service.EmptyStageExecutor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class SideCarConfig {
    @Bean
    public SideCarConfigurationProperties sideCarConfigurationProperties() {
        return new SideCarConfigurationProperties();
    }

    @Bean
    public EmptyStageExecutor emptyStageExecutor() {
        return new EmptyStageExecutor();
    }

    @ConfigurationProperties("sideCar")
    @Data
    public static class SideCarConfigurationProperties {
        private String host = "localhost";

        private int httpPort = 8080;

        private int grpcPort = 8080;

        private int serviceGrpcPort = 9091;

        private String serviceId;
    }
}
