package com.bragdev.frauddetection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    ReactiveUserDetailsServiceAutoConfiguration.class
})
@EnableScheduling
public class FrauddetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrauddetectionApplication.class, args);
    }
}
