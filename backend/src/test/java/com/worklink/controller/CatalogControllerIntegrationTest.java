package com.worklink.controller;

import com.worklink.config.CatalogSeedConfig;
import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.Role;
import com.worklink.repository.CategoryRepository;
import com.worklink.repository.SkillRepository;
import com.worklink.repository.UserRepository;
import com.worklink.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CatalogSeedConfig catalogSeedConfig;

    private String clientAuth;
    private String freelancerAuth;

    @BeforeEach
    void setUp() {
        // Seed catalogs for test environment
        catalogSeedConfig.seedCatalogs(categoryRepository, skillRepository);

        User client = userRepository.saveAndFlush(new User("Cat", "Client", "cat.client@example.com", "hash", Role.CLIENT, AccountStatus.ACTIVE));
        User freelancer = userRepository.saveAndFlush(new User("Cat", "Freelancer", "cat.freelancer@example.com", "hash", Role.FREELANCER, AccountStatus.ACTIVE));

        clientAuth = "Bearer " + jwtService.generateAccessToken(client);
        freelancerAuth = "Bearer " + jwtService.generateAccessToken(freelancer);
    }

    @Test
    void shouldReturnNonEmptyCategoriesWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, clientAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(9))))
                .andExpect(jsonPath("$[?(@.name == 'Web Development')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'UI/UX Design')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Mobile Development')]").exists());
    }

    @Test
    void shouldReturnNonEmptySkillsWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/skills")
                        .header(HttpHeaders.AUTHORIZATION, freelancerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(16))))
                .andExpect(jsonPath("$[?(@.name == 'Java')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'React')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Python')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'PostgreSQL')]").exists());
    }

    @Test
    void shouldRejectUnauthenticatedAccessToCatalogApis() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void catalogSeedingMustBeIdempotent() {
        long initialCategoryCount = categoryRepository.count();
        long initialSkillCount = skillRepository.count();

        // Run seed a second and third time
        catalogSeedConfig.seedCatalogs(categoryRepository, skillRepository);
        catalogSeedConfig.seedCatalogs(categoryRepository, skillRepository);

        assertEquals(initialCategoryCount, categoryRepository.count(), "Category count must remain unchanged after re-seeding");
        assertEquals(initialSkillCount, skillRepository.count(), "Skill count must remain unchanged after re-seeding");
    }
}
