package com.worklink.config;

import com.worklink.entity.Category;
import com.worklink.entity.Skill;
import com.worklink.repository.CategoryRepository;
import com.worklink.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
public class CatalogSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeedConfig.class);

    public record CategorySeedItem(String name, String description) {}
    public record SkillSeedItem(String name, String category) {}

    public static final List<CategorySeedItem> DEFAULT_CATEGORIES = List.of(
            new CategorySeedItem("Web Development", "Frontend, backend, full stack web systems, and modern APIs."),
            new CategorySeedItem("Mobile Development", "Native and cross-platform mobile apps for iOS and Android."),
            new CategorySeedItem("UI/UX Design", "Product design, user experience research, wireframing, and design systems."),
            new CategorySeedItem("Data Science", "Data analysis, statistical modeling, data warehousing, and business intelligence."),
            new CategorySeedItem("Machine Learning", "Machine learning engineering, deep learning, NLP, and AI integrations."),
            new CategorySeedItem("Software Testing", "Quality assurance, test automation, performance, and security testing."),
            new CategorySeedItem("Cloud & DevOps", "Cloud infrastructure, containerization, CI/CD pipelines, and site reliability."),
            new CategorySeedItem("Content Writing", "Technical documentation, copywriting, UX writing, and content strategy."),
            new CategorySeedItem("Digital Marketing", "Search engine optimization, paid marketing, analytics, and growth strategy.")
    );

    public static final List<SkillSeedItem> DEFAULT_SKILLS = List.of(
            new SkillSeedItem("Java", "Backend"),
            new SkillSeedItem("JavaScript", "Frontend"),
            new SkillSeedItem("TypeScript", "Frontend"),
            new SkillSeedItem("React", "Frontend"),
            new SkillSeedItem("Angular", "Frontend"),
            new SkillSeedItem("Node.js", "Backend"),
            new SkillSeedItem("Spring Boot", "Backend"),
            new SkillSeedItem("Python", "Backend"),
            new SkillSeedItem("SQL", "Database"),
            new SkillSeedItem("PostgreSQL", "Database"),
            new SkillSeedItem("MongoDB", "Database"),
            new SkillSeedItem("UI/UX Design", "Design"),
            new SkillSeedItem("Figma", "Design"),
            new SkillSeedItem("Docker", "DevOps"),
            new SkillSeedItem("AWS", "Cloud"),
            new SkillSeedItem("Git", "Tools")
    );

    @Bean
    @ConditionalOnProperty(prefix = "worklink.catalog-seed", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner catalogSeed(CategoryRepository categories, SkillRepository skills) {
        return args -> seedCatalogs(categories, skills);
    }

    @Transactional
    public void seedCatalogs(CategoryRepository categories, SkillRepository skills) {
        for (CategorySeedItem item : DEFAULT_CATEGORIES) {
            if (!categories.existsByNameIgnoreCase(item.name())) {
                categories.save(new Category(item.name(), item.description()));
                log.info("Seeded catalog category: {}", item.name());
            }
        }

        for (SkillSeedItem item : DEFAULT_SKILLS) {
            if (!skills.existsByNameIgnoreCase(item.name())) {
                skills.save(new Skill(item.name(), item.category()));
                log.info("Seeded catalog skill: {} ({})", item.name(), item.category());
            } else {
                skills.findByNameIgnoreCase(item.name()).ifPresent(existing -> {
                    if (existing.getCategory() == null && item.category() != null) {
                        existing.setCategory(item.category());
                        skills.save(existing);
                        log.info("Backfilled category for skill: {} -> {}", item.name(), item.category());
                    }
                });
            }
        }
    }
}

