package io.hydrosphere.serving.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class ManagerConfig {

    @Bean
    public ManagerConfigurationProperties managerConfigurationProperties() {
        return new ManagerConfigurationProperties();
    }

    @ConfigurationProperties("manager")
    @Data
    public static class ManagerConfigurationProperties {
        private String externalHost = "localhost";
    }
}
