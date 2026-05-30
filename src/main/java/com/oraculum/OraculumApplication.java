package com.oraculum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OraculumApplication {

    static void main(String[] args) {
        SpringApplication.run(OraculumApplication.class, args);
    }
}
