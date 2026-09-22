package com.arirahmat.helpdesk;

import com.arirahmat.helpdesk.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class HelpdeskApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpdeskApiApplication.class, args);
    }
}
