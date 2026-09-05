package com.worklink.controller;

import com.worklink.entity.Category;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.Skill;
import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.Role;
import com.worklink.repository.CategoryRepository;
import com.worklink.repository.ClientProfileRepository;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private ClientProfileRepository clients;
    @Autowired private CategoryRepository categories;
    @Autowired private SkillRepository skills;
    @Autowired private JwtService jwt;

    @Test
    void clientCanManageDraftAndFreelancerCanDiscoverOnlyPublishedProjects() throws Exception {
        User client = users.saveAndFlush(new User("Project", "Client", "project.client@example.com", "unused", Role.CLIENT, AccountStatus.ACTIVE));
        ClientProfile clientProfile = new ClientProfile(client, "Acme Studio", "", "Technology", "Remote", "https://acme.example");
        clientProfile.setContactInformation("hello@acme.example"); clients.saveAndFlush(clientProfile);
        User freelancer = users.saveAndFlush(new User("Project", "Freelancer", "project.freelancer@example.com", "unused", Role.FREELANCER, AccountStatus.ACTIVE));
        Category category = categories.saveAndFlush(new Category("Project test category", ""));
        Skill skill = skills.saveAndFlush(new Skill("Project test skill", ""));
        String clientAuth = "Bearer " + jwt.generateAccessToken(client);
        String freelancerAuth = "Bearer " + jwt.generateAccessToken(freelancer);
        String payload = payload(category.getId(), skill.getId(), "Marketplace API");

        String body = mockMvc.perform(post("/api/client/projects").header(HttpHeaders.AUTHORIZATION, clientAuth).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.skills[0].importance").value("REQUIRED"))
                .andReturn().getResponse().getContentAsString();
        Number idValue = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        long id = idValue.longValue();

        mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, freelancerAuth)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(put("/api/client/projects/{id}", id).header(HttpHeaders.AUTHORIZATION, clientAuth).contentType(MediaType.APPLICATION_JSON).content(payload(category.getId(), skill.getId(), "Updated Marketplace API")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated Marketplace API"));
        mockMvc.perform(patch("/api/client/projects/{id}/publish", id).header(HttpHeaders.AUTHORIZATION, clientAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, freelancerAuth)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(delete("/api/client/projects/{id}", id).header(HttpHeaders.AUTHORIZATION, clientAuth)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/client/projects").header(HttpHeaders.AUTHORIZATION, freelancerAuth)).andExpect(status().isForbidden());
    }

    private String payload(Long categoryId, Long skillId, String title) {
        return """
                {"title":"%s","description":"Build a reliable project marketplace API.","categoryId":%d,"requiredExperience":2,"budgetMin":1000,"budgetMax":2500,"deadline":"%s","complexity":"MEDIUM","deliverables":"REST endpoints and tests","skills":[{"skillId":%d,"importance":"REQUIRED"}]}
                """.formatted(title, categoryId, LocalDate.now().plusDays(30), skillId);
    }
}
