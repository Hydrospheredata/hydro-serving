package io.hydrosphere.serving;

import io.grpc.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 */
@SpringBootApplication
public class ServingApp {
    public static void main(String[] args) throws InterruptedException {
        Server server = SpringApplication.run(ServingApp.class, args).getBean(Server.class);
        server.awaitTermination();
    }
}