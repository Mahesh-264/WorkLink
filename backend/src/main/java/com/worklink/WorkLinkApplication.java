package com.worklink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.worklink.config.MatchingRankingWeights;
import com.worklink.config.ContextAwareMatchingWeights;

/**
 * Main entry point for the WorkLink application.
 * JPA Auditing enabled for automatic createdAt / updatedAt timestamps.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties({MatchingRankingWeights.class, ContextAwareMatchingWeights.class})
public class WorkLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkLinkApplication.class, args);
    }
}
