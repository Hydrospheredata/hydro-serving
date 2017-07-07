package io.hydrosphere.serving;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 */
@SpringBootApplication
public class ServingApp {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(ServingApp.class, args);
    }
}