package com.worklink.controller;

import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.FreelancerSkill;
import com.worklink.entity.PortfolioProject;
import com.worklink.entity.Skill;
import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.Role;
import com.worklink.entity.enums.SkillProficiency;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.FreelancerSkillRepository;
import com.worklink.repository.PortfolioProjectRepository;
import com.worklink.repository.SkillRepository;
import com.worklink.repository.UserRepository;
import com.worklink.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FreelancerCapabilityIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private FreelancerProfileRepository profileRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private FreelancerSkillRepository freelancerSkillRepository;
    @Autowired private PortfolioProjectRepository portfolioRepository;
    @Autowired private JwtService jwtService;

    @Test
    void freelancerCanListAssignUpdateAndRemoveSkillsWithoutDuplicates() throws Exception {
        FreelancerProfile freelancer = freelancer("skills.owner@example.com");
        Skill java = skill("Java", "Backend");
        Skill react = skill("React", "Frontend");
        String auth = bearer(freelancer.getUser());

        mockMvc.perform(get("/api/skills").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == %d)].name".formatted(java.getId())).value("Java"));
        mockMvc.perform(post("/api/freelancer/skills").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":%d,\"proficiency\":\"ADVANCED\"}".formatted(java.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.skillId").value(java.getId())).andExpect(jsonPath("$.proficiency").value("ADVANCED"));
        mockMvc.perform(post("/api/freelancer/skills").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":%d,\"proficiency\":\"EXPERT\"}".formatted(java.getId())))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/freelancer/skills/{id}", java.getId()).header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":%d,\"proficiency\":\"EXPERT\"}".formatted(java.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.proficiency").value("EXPERT"));
        mockMvc.perform(get("/api/freelancer/skills").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Java"));
        mockMvc.perform(delete("/api/freelancer/skills/{id}", java.getId()).header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/freelancer/skills").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":%d,\"proficiency\":\"BEGINNER\"}".formatted(react.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void skillEndpointsEnforceRoleOwnershipAndValidation() throws Exception {
        FreelancerProfile owner = freelancer("skills.first@example.com");
        FreelancerProfile other = freelancer("skills.second@example.com");
        Skill skill = skill("Kotlin", "Backend");
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(other, skill, SkillProficiency.EXPERT));
        User client = user("skills.client@example.com", Role.CLIENT);

        mockMvc.perform(get("/api/freelancer/skills")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/freelancer/skills").header(HttpHeaders.AUTHORIZATION, bearer(client))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/freelancer/skills/{id}", skill.getId()).header(HttpHeaders.AUTHORIZATION, bearer(owner.getUser()))).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/freelancer/skills").header(HttpHeaders.AUTHORIZATION, bearer(owner.getUser())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":999999,\"proficiency\":\"EXPERT\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/freelancer/skills").header(HttpHeaders.AUTHORIZATION, bearer(owner.getUser())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":%d}".formatted(skill.getId())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.proficiency").exists());
    }

    @Test
    void freelancerCanCreateUpdateAndDeletePortfolioWithOwnershipProtection() throws Exception {
        FreelancerProfile owner = freelancer("portfolio.owner@example.com");
        FreelancerProfile other = freelancer("portfolio.other@example.com");
        String auth = bearer(owner.getUser());
        mockMvc.perform(post("/api/freelancer/portfolio").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON).content(portfolioPayload("Matching Engine")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Matching Engine")).andExpect(jsonPath("$.repositoryUrl").value("https://github.com/example/project"));
        Long ownId = portfolioRepository.findByFreelancerId(owner.getId()).getFirst().getId();
        mockMvc.perform(put("/api/freelancer/portfolio/{id}", ownId).header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON).content(portfolioPayload("Updated Engine")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated Engine"));
        PortfolioProject otherItem = portfolioRepository.saveAndFlush(portfolio(other, "Private work"));
        mockMvc.perform(delete("/api/freelancer/portfolio/{id}", otherItem.getId()).header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/freelancer/portfolio").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Updated Engine"));
        mockMvc.perform(delete("/api/freelancer/portfolio/{id}", ownId).header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isNoContent());
    }

    @Test
    void portfolioEndpointsEnforceRoleAndValidation() throws Exception {
        FreelancerProfile freelancer = freelancer("portfolio.validation@example.com");
        User client = user("portfolio.client@example.com", Role.CLIENT);
        mockMvc.perform(get("/api/freelancer/portfolio")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/freelancer/portfolio").header(HttpHeaders.AUTHORIZATION, bearer(client))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/freelancer/portfolio").header(HttpHeaders.AUTHORIZATION, bearer(freelancer.getUser())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"projectUrl\":\"not-a-url\",\"completionDate\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.title").exists()).andExpect(jsonPath("$.fieldErrors.projectUrl").exists());
    }

    private User user(String email, Role role) { return userRepository.saveAndFlush(new User("Test", "User", email, "unused", role, AccountStatus.ACTIVE)); }
    private FreelancerProfile freelancer(String email) { User user = user(email, Role.FREELANCER); return profileRepository.saveAndFlush(new FreelancerProfile(user, "Engineer", "Bio", "Remote", 5, new BigDecimal("70"), "AVAILABLE")); }
    private Skill skill(String name, String category) { return skillRepository.findByNameIgnoreCase(name).orElseGet(() -> skillRepository.saveAndFlush(new Skill(name, category))); }
    private String bearer(User user) { return "Bearer " + jwtService.generateAccessToken(user); }
    private String portfolioPayload(String title) { return "{\"title\":\"%s\",\"description\":\"A practical system\",\"projectUrl\":\"https://example.com/project\",\"repositoryUrl\":\"https://github.com/example/project\",\"technologiesUsed\":\"Java, React, PostgreSQL\",\"role\":\"Lead developer\",\"completionDate\":\"2025-05-01\",\"imageUrl\":\"https://example.com/image.png\"}".formatted(title); }
    private PortfolioProject portfolio(FreelancerProfile freelancer, String title) { PortfolioProject project = new PortfolioProject(freelancer, title, "Description", "https://example.com", "Java", null); project.setCompletionDate(LocalDate.of(2025, 1, 1)); return project; }
}
