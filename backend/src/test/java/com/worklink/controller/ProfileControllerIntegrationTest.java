package com.worklink.controller;

import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.Role;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    @Test
    void clientCanCreateRetrieveAndUpdateOwnProfile() throws Exception {
        User client = user("client.profile@example.com", Role.CLIENT);
        String authorization = bearer(client);

        mockMvc.perform(post("/api/client/profile").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(clientPayload("Acme Labs")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Acme Labs"))
                .andExpect(jsonPath("$.contactInformation").value("hello@acme.example"));

        mockMvc.perform(get("/api/client/profile").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Labs"));

        mockMvc.perform(put("/api/client/profile").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(clientPayload("Acme Updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Updated"));
    }

    @Test
    void freelancerCanCreateRetrieveAndUpdateOwnProfile() throws Exception {
        User freelancer = user("freelancer.profile@example.com", Role.FREELANCER);
        String authorization = bearer(freelancer);

        mockMvc.perform(post("/api/freelancer/profile").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(freelancerPayload("Java Engineer")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.professionalHeadline").value("Java Engineer"))
                .andExpect(jsonPath("$.yearsExperience").value(5));

        mockMvc.perform(get("/api/freelancer/profile").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("AVAILABLE"));

        mockMvc.perform(put("/api/freelancer/profile").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(freelancerPayload("Senior Java Engineer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalHeadline").value("Senior Java Engineer"));
    }

    @Test
    void profileCreationPreventsDuplicatesAndValidatesInput() throws Exception {
        User client = user("duplicate.profile@example.com", Role.CLIENT);
        String authorization = bearer(client);

        mockMvc.perform(post("/api/client/profile").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(clientPayload("Only Once")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/client/profile").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(clientPayload("Duplicate")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A profile already exists for this account"));

        User invalidClient = user("invalid.profile@example.com", Role.CLIENT);
        mockMvc.perform(post("/api/client/profile").header(HttpHeaders.AUTHORIZATION, bearer(invalidClient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"","website":"not-a-url","contactInformation":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.companyName").exists())
                .andExpect(jsonPath("$.fieldErrors.website").exists())
                .andExpect(jsonPath("$.fieldErrors.contactInformation").exists());
    }

    @Test
    void profilesAreStrictlyScopedToAuthenticatedOwner() throws Exception {
        User firstClient = user("first.owner@example.com", Role.CLIENT);
        User secondClient = user("second.owner@example.com", Role.CLIENT);
        mockMvc.perform(post("/api/client/profile").header(HttpHeaders.AUTHORIZATION, bearer(firstClient))
                        .contentType(MediaType.APPLICATION_JSON).content(clientPayload("First Company")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/client/profile").header(HttpHeaders.AUTHORIZATION, bearer(secondClient))
                        .contentType(MediaType.APPLICATION_JSON).content(clientPayload("Second Company")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/client/profile").header(HttpHeaders.AUTHORIZATION, bearer(firstClient)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.companyName").value("First Company"));
        mockMvc.perform(get("/api/client/profile").header(HttpHeaders.AUTHORIZATION, bearer(secondClient)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.companyName").value("Second Company"));
    }

    @Test
    void missingProfileAndWrongRoleAreHandledSafely() throws Exception {
        User client = user("missing.profile@example.com", Role.CLIENT);
        User freelancer = user("wrong-role.profile@example.com", Role.FREELANCER);

        mockMvc.perform(get("/api/client/profile").header(HttpHeaders.AUTHORIZATION, bearer(client)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Profile not found"));
        mockMvc.perform(get("/api/client/profile").header(HttpHeaders.AUTHORIZATION, bearer(freelancer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/freelancer/profile").header(HttpHeaders.AUTHORIZATION, bearer(client)))
                .andExpect(status().isForbidden());
    }

    private User user(String email, Role role) {
        return userRepository.saveAndFlush(new User("Profile", "User", email, "unused", role, AccountStatus.ACTIVE));
    }

    private String bearer(User user) { return "Bearer " + jwtService.generateAccessToken(user); }

    private String clientPayload(String companyName) {
        return """
                {"companyName":"%s","companyDescription":"Product studio","industry":"Technology","website":"https://acme.example","location":"Bengaluru","contactInformation":"hello@acme.example"}
                """.formatted(companyName);
    }

    private String freelancerPayload(String headline) {
        return """
                {"professionalHeadline":"%s","bio":"Backend engineer","location":"Pune","yearsExperience":5,"hourlyRate":75.00,"availability":"AVAILABLE","resumeInformation":"https://example.com/resume","profilePhotoUrl":"https://example.com/photo.jpg"}
                """.formatted(headline);
    }
}
