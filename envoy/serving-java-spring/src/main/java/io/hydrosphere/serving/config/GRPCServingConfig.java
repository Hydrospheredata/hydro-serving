package io.hydrosphere.serving.config;

import io.grpc.*;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.hydrosphere.serving.proto.Health;
import io.hydrosphere.serving.proto.HealthStatus;
import io.hydrosphere.serving.proto.ServingServiceGrpc;
import io.hydrosphere.serving.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 *
 */
@Configuration
public class GRPCServingConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties;

    @Bean
    public ServingConfigurationProperties servingConfigurationProperties() {
        return new ServingConfigurationProperties();
    }

    @Bean
    public ServingServiceGrpc.ServingServiceStub servingServiceStub() {
        ManagedChannel managedChannel = NettyChannelBuilder
                .forAddress(sideCarConfigurationProperties.getHost(),
                        sideCarConfigurationProperties.getGrpcPort())
                .usePlaintext(true)
                .build();
        Channel channel = ClientInterceptors.intercept(managedChannel,
                new AuthorityReplacerInterceptor(), new TracingHeaderInterceptor());
        return ServingServiceGrpc.newStub(channel);
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server server() {
        return NettyServerBuilder.forPort(sideCarConfigurationProperties.getServiceGrpcPort())
                .addService(ServerInterceptors.intercept(new ServingServiceImpl(servingServiceStub(),
                        stageExecutor(),
                        executor(),
                        sideCarConfigurationProperties.getServiceId()), new TracingHeaderInterceptor()))
                .addService(new HealthServiceImpl(getCheckers()))
                .build();
    }

    private Map<String, HealthChecker> getCheckers() {
        Map<String, HealthIndicator> map = applicationContext.getBeansOfType(HealthIndicator.class);
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, HealthChecker> result = new HashMap<>();
        map.forEach((k, v) -> {
            result.put(k, () -> {
                org.springframework.boot.actuate.health.Health health = v.health();
                Health.Builder healthBuilder = Health.newBuilder()
                        .setDetails(String.valueOf(health.getDetails()));
                switch (health.getStatus().getCode()) {
                    case "UP":
                        healthBuilder.setStatus(HealthStatus.UP);
                        break;
                    default:
                        healthBuilder.setStatus(HealthStatus.DOWN);
                        break;
                }
                return healthBuilder.build();
            });
        });
        return result;
    }

    @Bean
    public StageExecutor stageExecutor() {
        try {
            Class clazz = Class.forName(servingConfigurationProperties().getStageExecutorClass());
            return (StageExecutor) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public Executor executor() {
        return ForkJoinPool.commonPool();
    }
}
