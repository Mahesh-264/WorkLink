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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void validClientJwtAuthenticatesAndProvidesClientAuthority() throws Exception {
        User client = persistUser("client.jwt@example.com", Role.CLIENT, AccountStatus.ACTIVE);

        mockMvc.perform(get("/api/test/authenticated").header(HttpHeaders.AUTHORIZATION, bearerFor(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(client.getId()))
                .andExpect(jsonPath("$.email").value("client.jwt@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"));

        mockMvc.perform(get("/api/test/client-only").header(HttpHeaders.AUTHORIZATION, bearerFor(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void validFreelancerJwtAuthenticatesAndHasFreelancerAuthority() throws Exception {
        User freelancer = persistUser("freelancer.jwt@example.com", Role.FREELANCER, AccountStatus.ACTIVE);

        mockMvc.perform(get("/api/test/authenticated").header(HttpHeaders.AUTHORIZATION, bearerFor(freelancer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("freelancer.jwt@example.com"))
                .andExpect(jsonPath("$.role").value("FREELANCER"));
    }

    @Test
    void freelancerIsForbiddenFromClientOnlyEndpoint() throws Exception {
        User freelancer = persistUser("forbidden.jwt@example.com", Role.FREELANCER, AccountStatus.ACTIVE);

        mockMvc.perform(get("/api/test/client-only").header(HttpHeaders.AUTHORIZATION, bearerFor(freelancer)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void missingJwtIsRejectedForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/authenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void malformedJwtIsRejected() throws Exception {
        mockMvc.perform(get("/api/test/authenticated").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid access token"));
    }

    @Test
    void expiredJwtIsRejected() throws Exception {
        User user = persistUser("expired.jwt@example.com", Role.CLIENT, AccountStatus.ACTIVE);
        Instant issuedAt = Instant.now().minusSeconds(7_200);
        String expiredToken = jwtService.generateAccessToken(user, issuedAt, issuedAt.plusSeconds(60));

        mockMvc.perform(get("/api/test/authenticated").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access token has expired"));
    }

    @Test
    void inactiveUserJwtIsRejected() throws Exception {
        User user = persistUser("inactive.jwt@example.com", Role.CLIENT, AccountStatus.ACTIVE);
        String token = bearerFor(user);
        user.setAccountStatus(AccountStatus.INACTIVE);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/test/authenticated").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is not available"));
    }

    private User persistUser(String email, Role role, AccountStatus status) {
        return userRepository.saveAndFlush(new User("JWT", "User", email, "not-used-in-jwt-tests", role, status));
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}
