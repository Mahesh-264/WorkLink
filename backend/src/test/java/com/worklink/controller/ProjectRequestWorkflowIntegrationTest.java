package com.worklink.controller;

import com.jayway.jsonpath.JsonPath;
import com.worklink.entity.Category;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.Project;
import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.ProjectComplexity;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.Role;
import com.worklink.repository.CategoryRepository;
import com.worklink.repository.ClientProfileRepository;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.ProjectRepository;
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
class ProjectRequestWorkflowIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired ClientProfileRepository clients;
    @Autowired FreelancerProfileRepository freelancers;
    @Autowired ProjectRepository projects;
    @Autowired CategoryRepository categories;
    @Autowired JwtService jwt;

    @Test
    void publishedProjectRequestCanBeAcceptedOnceAndBecomesActiveForBothParties() throws Exception {
        User clientUser = users.save(new User("Owner", "Client", "workflow.client@example.com", "x", Role.CLIENT, AccountStatus.ACTIVE));
        ClientProfile client = clients.save(new ClientProfile(clientUser, "Workflow Co", "", "Technology", "Remote", "https://workflow.example"));
        client.setContactInformation("contact@workflow.example");
        clients.save(client);
        User firstUser = users.save(new User("First", "Freelancer", "workflow.first@example.com", "x", Role.FREELANCER, AccountStatus.ACTIVE));
        freelancers.save(new FreelancerProfile(firstUser, "Backend engineer", "", "Remote", 4, new BigDecimal("50"), "AVAILABLE"));
        User secondUser = users.save(new User("Second", "Freelancer", "workflow.second@example.com", "x", Role.FREELANCER, AccountStatus.ACTIVE));
        freelancers.save(new FreelancerProfile(secondUser, "Frontend engineer", "", "Remote", 3, new BigDecimal("50"), "AVAILABLE"));
        Category category = categories.save(new Category("Workflow category", ""));
        Project project = projects.save(new Project(client, "Workflow project", "Build the workflow", category, new BigDecimal("1000"), new BigDecimal("2000"), 2, LocalDate.now().plusDays(30), ProjectComplexity.MEDIUM, ProjectStatus.PUBLISHED));

        String firstAuth = auth(firstUser), secondAuth = auth(secondUser), clientAuth = auth(clientUser);
        long firstProposal = submit(project.getId(), firstAuth, "I will plan, implement, and test the workflow.");
        long secondProposal = submit(project.getId(), secondAuth, "I will deliver the requested interface.");
        mockMvc.perform(post("/api/projects/{id}/proposals", project.getId()).header(HttpHeaders.AUTHORIZATION, firstAuth).contentType(MediaType.APPLICATION_JSON).content(request("Duplicate")))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/client/projects/{id}/proposals", project.getId()).header(HttpHeaders.AUTHORIZATION, clientAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].coverLetter").value("I will plan, implement, and test the workflow."));
        mockMvc.perform(patch("/api/client/projects/{projectId}/proposals/{proposalId}/status", project.getId(), firstProposal).header(HttpHeaders.AUTHORIZATION, clientAuth).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(get("/api/client/projects/active").header(HttpHeaders.AUTHORIZATION, clientAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(project.getId())).andExpect(jsonPath("$[0].acceptedFreelancerName").value("First Freelancer"));
        mockMvc.perform(get("/api/projects/accepted").header(HttpHeaders.AUTHORIZATION, firstAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(project.getId()));
        mockMvc.perform(get("/api/freelancer/proposals").header(HttpHeaders.AUTHORIZATION, secondAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("REJECTED"));
        mockMvc.perform(patch("/api/client/projects/{projectId}/proposals/{proposalId}/status", project.getId(), secondProposal).header(HttpHeaders.AUTHORIZATION, clientAuth).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isBadRequest());
    }

    private long submit(Long projectId, String auth, String approach) throws Exception {
        String response = mockMvc.perform(post("/api/projects/{id}/proposals", projectId).header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON).content(request(approach)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.coverLetter").value(approach)).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
    private String request(String approach) { return "{\"coverLetter\":\"" + approach + "\",\"proposedBudget\":1500,\"estimatedDuration\":14}"; }
    private String auth(User user) { return "Bearer " + jwt.generateAccessToken(user); }
}
