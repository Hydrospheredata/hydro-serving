package io.hydrosphere.serving.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@ConfigurationProperties("serving")
@Data
public class ServingConfigurationProperties {

    private String stageExecutorClass;

    private String serviceName;
}
