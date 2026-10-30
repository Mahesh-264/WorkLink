package com.worklink.security;

import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.Role;
import com.worklink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CorsAndSecurityIntegrationTest {

    private static final String VERCEL_ORIGIN = "https://work-link-ochre.vercel.app";
    private static final String LOCALHOST_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationEndpointIsAccessibleWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Public",
                                  "lastName": "User",
                                  "email": "public.register@example.com",
                                  "password": "password123",
                                  "role": "CLIENT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("public.register@example.com"));
    }

    @Test
    void registrationDoesNotRequireAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "NoAuth",
                                  "lastName": "Header",
                                  "email": "noauth.header@example.com",
                                  "password": "password123",
                                  "role": "FREELANCER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("noauth.header@example.com"));
    }

    @Test
    void loginEndpointIsAccessibleWithoutJwt() throws Exception {
        userRepository.save(new User("Test", "Login", "public.login@example.com",
                passwordEncoder.encode("password123"), Role.CLIENT, AccountStatus.ACTIVE));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "public.login@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void protectedEndpointsStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/test/authenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void invalidJwtIsRejectedOnProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/test/authenticated")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer totally-invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid access token"));
    }

    @Test
    void corsAllowsDeployedVercelOriginOnGetRequest() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", VERCEL_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsAllowsLocalhostOrigin() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header(HttpHeaders.ORIGIN, LOCALHOST_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", LOCALHOST_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsPreflightRequestWorksForRegistration() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Authorization, Accept"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", VERCEL_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsPreflightRequestWorksForLogin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", VERCEL_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }

    @Test
    void corsPreflightRequestWorksForProtectedEndpoints() throws Exception {
        mockMvc.perform(options("/api/test/authenticated")
                        .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", VERCEL_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")));
    }

    @Test
    void corsRejectsUnauthorizedOrigins() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header(HttpHeaders.ORIGIN, "https://malicious-origin.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
