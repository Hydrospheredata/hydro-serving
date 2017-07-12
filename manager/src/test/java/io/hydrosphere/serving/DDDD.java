package io.hydrosphere.serving;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.mount.Mount;
import com.spotify.docker.client.messages.swarm.*;
import org.junit.Test;

import static io.hydrosphere.serving.clouddriver.swarm.SwarmRuntimeDeployService.*;

/**
 *
 */
public class DDDD {
    /*@Test
    public void test() throws Exception {
        DockerClient client = DefaultDockerClient.fromEnv().build();

        client.createService(ServiceSpec.builder()
                .name("hydro-serving-gateway")
                .networks(NetworkAttachmentConfig.builder()
                        .target("net1")
                        .build())
                .addLabel(LABEL_MODEL_NAME, "dummy")
                .addLabel(LABEL_MODEL_TYPE, "dummy")
                .addLabel(LABEL_MODEL_VERSION, "222")
                .addLabel(LABEL_RUNTIME_TYPE, "gateway")
                .addLabel(LABEL_HTTP_PORT, "8080")
                .addLabel(LABEL_APP_HTTP_PORT, "9090")
                .endpointSpec(EndpointSpec.builder()
                        *//*.addPort(PortConfig.builder()
                                .publishedPort(8082)
                                .targetPort(8082)
                                .publishMode(PortConfig.PortConfigPublishMode.HOST)
                                .build())*//*
                        .addPort(PortConfig.builder()
                                .publishedPort(8081)
                                .targetPort(8080)
                                .publishMode(PortConfig.PortConfigPublishMode.HOST)
                                .build())
                        *//*.addPort(PortConfig.builder()
                                .publishedPort(9090)
                                .targetPort(9090)
                                .publishMode(PortConfig.PortConfigPublishMode.HOST)
                                .build())*//*
                        .build())
                .taskTemplate(TaskSpec.builder()
                        .containerSpec(ContainerSpec.builder()
                                .image("hydro-serving/gateway:1.0-SNAPSHOT")
                                *//*.mounts(Mount.builder()
                                        .source("/var/run/docker.sock")
                                        .target("/var/run/docker.sock")
                                        .build())*//*
                                .env("MANAGER_HOST=192.168.90.117")
                                .build())
                        .build())
                .build());

    }*/
}
