package io.hydrosphere.serving.config;

import com.ecwid.consul.v1.ConsulClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class ConsulConfig {
    @Bean
    public ConsulClient consulClient() {
        return new ConsulClient(consulConfigurationProperties().getHost(),
                consulConfigurationProperties().getPort());
    }

    @Bean
    public ConsulConfigurationProperties consulConfigurationProperties() {
        return new ConsulConfigurationProperties();
    }

    @ConfigurationProperties("consul")
    @Data
    public static class ConsulConfigurationProperties {
        private String host = "localhost";
        private int port = 8500;
    }
}
