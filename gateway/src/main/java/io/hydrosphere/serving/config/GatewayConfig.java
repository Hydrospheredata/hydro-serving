package io.hydrosphere.serving.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class GatewayConfig {

    @Bean
    public GatewayConfigurationProperties gatewayConfigurationProperties() {
        return new GatewayConfigurationProperties();
    }

    @ConfigurationProperties("gateway")
    @Data
    public static class GatewayConfigurationProperties {
        private String managerHost;

        private int managerPort;
    }
}
