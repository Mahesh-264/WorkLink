package com.worklink.controller;

import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.Role;
import com.worklink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerCreatesClientWithHashedPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace","email":"ADA.AUTH@EXAMPLE.COM","password":"secure-password","role":"CLIENT"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ada.auth@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User user = userRepository.findByEmail("ada.auth@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("secure-password", user.getPassword()));
    }

    @Test
    void registrationDoesNotRequireAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"NoAuth","lastName":"User","email":"noauth.auth@example.com","password":"secure-password","role":"CLIENT"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("noauth.auth@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void registrationAccessibleEvenWithMalformedAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer stale-or-malformed-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Stale","lastName":"Token","email":"stale.auth@example.com","password":"secure-password","role":"FREELANCER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("stale.auth@example.com"))
                .andExpect(jsonPath("$.role").value("FREELANCER"));
    }

    @Test
    void loginAcceptsValidFreelancerCredentials() throws Exception {
        userRepository.save(new User("Grace", "Hopper", "grace.auth@example.com",
                passwordEncoder.encode("secure-password"), Role.FREELANCER, AccountStatus.ACTIVE));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"GRACE.AUTH@EXAMPLE.COM","password":"secure-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("grace.auth@example.com"))
                .andExpect(jsonPath("$.role").value("FREELANCER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginAcceptsValidClientCredentialsAndReturnsJwt() throws Exception {
        userRepository.save(new User("Lin", "Client", "lin.client@example.com",
                passwordEncoder.encode("secure-password"), Role.CLIENT, AccountStatus.ACTIVE));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"lin.client@example.com","password":"secure-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void registrationRejectsAdminAndValidationErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Admin","lastName":"User","email":"admin.auth@example.com","password":"secure-password","role":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only CLIENT and FREELANCER roles may self-register"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"User","email":"not-an-email","password":"short","role":"CLIENT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown.auth@example.com","password":"secure-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void registrationRejectsDuplicateEmail() throws Exception {
        userRepository.save(new User("Existing", "Client", "duplicate.auth@example.com",
                passwordEncoder.encode("secure-password"), Role.CLIENT, AccountStatus.ACTIVE));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Another","lastName":"Client","email":"DUPLICATE.AUTH@EXAMPLE.COM","password":"secure-password","role":"CLIENT"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }
}
