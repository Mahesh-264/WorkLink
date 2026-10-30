package com.worklink.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MatchingEvaluationControllerSecurityTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void evaluationEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/evaluation/matching")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void ordinaryClientCannotReadResearchEvaluationData() throws Exception {
        mockMvc.perform(get("/api/evaluation/matching")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FREELANCER")
    void ordinaryFreelancerCannotReadResearchEvaluationData() throws Exception {
        mockMvc.perform(get("/api/evaluation/matching/summary")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanReadEvaluationRecordsAndSummary() throws Exception {
        mockMvc.perform(get("/api/evaluation/matching")).andExpect(status().isOk());
        mockMvc.perform(get("/api/evaluation/matching/summary")).andExpect(status().isOk());
    }
}
