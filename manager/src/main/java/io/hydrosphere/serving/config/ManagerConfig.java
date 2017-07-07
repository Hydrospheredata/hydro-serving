package io.hydrosphere.serving.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 */
@Configuration
public class ManagerConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedHeaders("*")
                        .maxAge(3600)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }

    @Bean
    public ManagerConfigurationProperties managerConfigurationProperties() {
        return new ManagerConfigurationProperties();
    }

    @ConfigurationProperties("manager")
    @Data
    public static class ManagerConfigurationProperties {
        private String managerServiceName = "hydro-serving-manager";

        private String gatewayServiceName = "hydro-serving-gateway";

        private String repositoryServiceName = "hydro-serving-repository";

        private String exposedHost;

        private int exposedPort;
    }
}
