package com.worklink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main entry point for the WorkLink application.
 * JPA Auditing enabled for automatic createdAt / updatedAt timestamps.
 */
@SpringBootApplication
@EnableJpaAuditing
public class WorkLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkLinkApplication.class, args);
    }
}
