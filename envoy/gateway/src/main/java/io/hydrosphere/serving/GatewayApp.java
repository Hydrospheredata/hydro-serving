package io.hydrosphere.serving;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 */
@SpringBootApplication
public class GatewayApp {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(GatewayApp.class, args);
    }
}
