package com.worklink.controller;

import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.Project;
import com.worklink.entity.User;
import com.worklink.entity.TrustHistory;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.ProjectComplexity;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.Role;
import com.worklink.repository.ClientProfileRepository;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.UserRepository;
import com.worklink.repository.TrustHistoryRepository;
import com.worklink.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchingControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ClientProfileRepository clientProfileRepository;
    @Autowired private FreelancerProfileRepository freelancerProfileRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private JwtService jwtService;
    @Autowired private TrustHistoryRepository trustHistoryRepository;

    private User clientUser;
    private User otherClientUser;
    private User freelancerUser;
    private User lowScoreFreelancerUser;
    private User inactiveFreelancerUser;
    private Project project;
    private String clientToken;
    private String otherClientToken;
    private String freelancerToken;

    @BeforeEach
    void setUp() {
        clientUser = userRepository.saveAndFlush(new User("Match", "Client", "client.match@test.com", "hash", Role.CLIENT, AccountStatus.ACTIVE));
        ClientProfile client = clientProfileRepository.saveAndFlush(new ClientProfile(clientUser, "Test Corp", "desc", "Tech", "NYC", "https://test.com"));
        clientToken = bearerFor(clientUser);

        otherClientUser = userRepository.saveAndFlush(new User("Other", "Client", "other.match@test.com", "hash", Role.CLIENT, AccountStatus.ACTIVE));
        clientProfileRepository.saveAndFlush(new ClientProfile(otherClientUser, "Other Corp", "desc", "Tech", "LA", "https://other.com"));
        otherClientToken = bearerFor(otherClientUser);

        freelancerUser = userRepository.saveAndFlush(new User("Match", "Freelancer", "freelancer.match@test.com", "hash", Role.FREELANCER, AccountStatus.ACTIVE));
        freelancerProfileRepository.saveAndFlush(new FreelancerProfile(freelancerUser, "Developer", "Bio", "Remote", 5, new BigDecimal("50"), "AVAILABLE"));
        freelancerToken = bearerFor(freelancerUser);

        lowScoreFreelancerUser = userRepository.saveAndFlush(new User("Low", "Match", "low.match@test.com", "hash", Role.FREELANCER, AccountStatus.ACTIVE));
        freelancerProfileRepository.saveAndFlush(new FreelancerProfile(lowScoreFreelancerUser, "Junior developer", "Bio", "Remote", 0, new BigDecimal("10000"), "AVAILABLE"));
        inactiveFreelancerUser = userRepository.saveAndFlush(new User("Inactive", "Match", "inactive.match@test.com", "hash", Role.FREELANCER, AccountStatus.INACTIVE));
        freelancerProfileRepository.saveAndFlush(new FreelancerProfile(inactiveFreelancerUser, "Inactive developer", "Bio", "Remote", 20, new BigDecimal("1500"), "AVAILABLE"));

        project = projectRepository.saveAndFlush(new Project(
                client, "Test Project", "Desc", null,
                new BigDecimal("1000"), new BigDecimal("2000"),
                1, LocalDate.now().plusMonths(1), ProjectComplexity.LOW, ProjectStatus.PUBLISHED));
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user);
    }

    // --- CLIENT ENDPOINT TESTS ---

    @Test
    void getMatches_ownsProject_returnsMatches() throws Exception {
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/matches")
                        .header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].freelancerHeadline").exists())
                .andExpect(jsonPath("$[0].compatibilityScore").exists())
                .andExpect(jsonPath("$[0].finalRankingScore").exists())
                .andExpect(jsonPath("$[0].trustAvailability").value("UNAVAILABLE"));
    }

    @Test
    void getMatches_doesNotOwnProject_returnsNotFoundOrForbidden() throws Exception {
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/matches")
                        .header(HttpHeaders.AUTHORIZATION, otherClientToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMatches_freelancerRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/matches")
                        .header(HttpHeaders.AUTHORIZATION, freelancerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMatches_withLimit_clampsToSafeRange() throws Exception {
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/matches?limit=1")
                        .header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].freelancerId").value(freelancerProfileRepository.findByUserId(freelancerUser.getId()).orElseThrow().getId()));
    }

    @Test
    void getMatches_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/matches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contextAwareMatches_requiresAuthenticationAndClientRole() throws Exception {
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/context-aware-matches"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/context-aware-matches")
                        .header(HttpHeaders.AUTHORIZATION, freelancerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void contextAwareMatches_isIndependentAndUsesOwnerAndLimit() throws Exception {
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/context-aware-matches?limit=1")
                        .header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].model").value("worklink_context_aware"))
                .andExpect(jsonPath("$[0].advancedMatchScore").exists())
                .andExpect(jsonPath("$[0].semanticAvailability").exists());
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/context-aware-matches")
                        .header(HttpHeaders.AUTHORIZATION, otherClientToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void contextAwareFreelancerEndpointReturnsOnlyAuthenticatedFreelancer() throws Exception {
        Long ownId = freelancerProfileRepository.findByUserId(freelancerUser.getId()).orElseThrow().getId();
        mockMvc.perform(get("/api/freelancer/projects/" + project.getId() + "/context-aware-match")
                        .header(HttpHeaders.AUTHORIZATION, freelancerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freelancerId").value(ownId))
                .andExpect(jsonPath("$.model").value("worklink_context_aware"))
                .andExpect(jsonPath("$.trustAvailability").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.performanceAvailability").value("UNAVAILABLE"));
        mockMvc.perform(get("/api/freelancer/projects/" + project.getId() + "/context-aware-match"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/freelancer/projects/" + project.getId() + "/context-aware-match")
                        .header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMatches_excludesInactiveFreelancersAndReturnsDescendingRanks() throws Exception {
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/matches?limit=10")
                        .header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[0].compatibilityScore").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$[?(@.freelancerName == 'Inactive Match')]").isEmpty());
    }

    @Test
    void getMatches_draftProject_isRejected() throws Exception {
        project.setStatus(ProjectStatus.DRAFT);
        projectRepository.saveAndFlush(project);
        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/matches")
                        .header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMatches_noEligibleFreelancers_returnsEmptyArray() throws Exception {
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FREELANCER)
                .forEach(u -> {
                    u.setAccountStatus(AccountStatus.INACTIVE);
                    userRepository.save(u);
                });
        userRepository.flush();

        mockMvc.perform(get("/api/client/projects/" + project.getId() + "/matches?limit=-1")
                        .header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- FREELANCER ENDPOINT TESTS ---

    @Test
    void getMyMatch_publishedProject_returnsMatchResult() throws Exception {
        Long ownId=freelancerProfileRepository.findByUserId(freelancerUser.getId()).orElseThrow().getId();
        mockMvc.perform(get("/api/freelancer/projects/" + project.getId() + "/match")
                        .header(HttpHeaders.AUTHORIZATION, freelancerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freelancerId").value(ownId))
                .andExpect(jsonPath("$.compatibilityScore").exists())
                .andExpect(jsonPath("$.finalRankingScore").exists())
                .andExpect(jsonPath("$.trustAvailability").value("UNAVAILABLE"));
    }

    @Test
    void getMyMatch_doesNotExposeAnotherFreelancersTrustSnapshot() throws Exception {
        FreelancerProfile other=freelancerProfileRepository.findByUserId(lowScoreFreelancerUser.getId()).orElseThrow();
        TrustHistory otherTrust=new TrustHistory(); otherTrust.setFreelancer(other); otherTrust.setOverallTrustScore(99.0);
        otherTrust.setProjectsConsidered(4); otherTrust.setCalculatedAt(LocalDateTime.now()); trustHistoryRepository.saveAndFlush(otherTrust);

        mockMvc.perform(get("/api/freelancer/projects/" + project.getId() + "/match")
                        .header(HttpHeaders.AUTHORIZATION, freelancerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freelancerId").value(freelancerProfileRepository.findByUserId(freelancerUser.getId()).orElseThrow().getId()))
                .andExpect(jsonPath("$.trustScore").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.trustAvailability").value("UNAVAILABLE"));
    }

    @Test
    void getMyMatch_usesOwnLatestPersistedTrustSnapshot() throws Exception {
        FreelancerProfile own=freelancerProfileRepository.findByUserId(freelancerUser.getId()).orElseThrow();
        TrustHistory snapshot=new TrustHistory(); snapshot.setFreelancer(own); snapshot.setOverallTrustScore(78.0);
        snapshot.setProjectsConsidered(2); snapshot.setCalculatedAt(LocalDateTime.now()); trustHistoryRepository.saveAndFlush(snapshot);
        mockMvc.perform(get("/api/freelancer/projects/" + project.getId() + "/match")
                        .header(HttpHeaders.AUTHORIZATION, freelancerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustScore").value(78.0))
                .andExpect(jsonPath("$.trustAvailability").value("AVAILABLE"))
                .andExpect(jsonPath("$.rankingExplanation").exists());
    }

    @Test
    void getMyMatch_draftProject_returnsNotFound() throws Exception {
        Project draftProject = projectRepository.saveAndFlush(new Project(
                project.getClient(), "Draft", "Desc", null,
                new BigDecimal("1000"), new BigDecimal("2000"),
                1, LocalDate.now().plusMonths(1), ProjectComplexity.LOW, ProjectStatus.DRAFT));

        mockMvc.perform(get("/api/freelancer/projects/" + draftProject.getId() + "/match")
                        .header(HttpHeaders.AUTHORIZATION, freelancerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyMatch_clientRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/freelancer/projects/" + project.getId() + "/match")
                        .header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isForbidden());
    }
}
