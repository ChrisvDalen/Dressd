package com.dressd.wardrobe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WardrobeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WardrobeServiceApplication.class, args);
    }
}
