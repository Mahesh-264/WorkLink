package com.worklink.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Keeps PostgreSQL's enum-derived check constraint aligned with the expanded project lifecycle. */
@Configuration
public class ProjectStatusSchemaConfig {

    @Bean
    CommandLineRunner alignProjectStatusConstraint(JdbcTemplate jdbcTemplate) {
        return arguments -> {
            jdbcTemplate.execute("ALTER TABLE projects DROP CONSTRAINT IF EXISTS projects_status_check");
            jdbcTemplate.execute("""
                    ALTER TABLE projects ADD CONSTRAINT projects_status_check
                    CHECK (status IN ('DRAFT', 'PUBLISHED', 'OPEN', 'IN_REVIEW', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'CLOSED'))
                    """);
            jdbcTemplate.execute("UPDATE proposals SET status = 'SUBMITTED' WHERE status = 'PENDING'");
            jdbcTemplate.execute("ALTER TABLE proposals DROP CONSTRAINT IF EXISTS proposals_status_check");
            jdbcTemplate.execute("ALTER TABLE proposals ADD CONSTRAINT proposals_status_check CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'SHORTLISTED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'))");
            jdbcTemplate.execute("ALTER TABLE project_skills DROP CONSTRAINT IF EXISTS project_skills_importance_check");
            jdbcTemplate.execute("ALTER TABLE project_skills ADD CONSTRAINT project_skills_importance_check CHECK (importance IN ('REQUIRED', 'IMPORTANT', 'PREFERRED'))");
            
            // Block 5 constraints
            jdbcTemplate.execute("ALTER TABLE milestones DROP CONSTRAINT IF EXISTS milestones_status_check");
            jdbcTemplate.execute("ALTER TABLE milestones ADD CONSTRAINT milestones_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'SUBMITTED', 'REVISION_REQUESTED', 'APPROVED'))");
            jdbcTemplate.execute("ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_rating_check");
            jdbcTemplate.execute("ALTER TABLE reviews ADD CONSTRAINT reviews_rating_check CHECK (rating >= 1 AND rating <= 5)");
        };
    }
}
