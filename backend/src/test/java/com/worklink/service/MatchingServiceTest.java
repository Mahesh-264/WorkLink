package com.worklink.service;

import com.worklink.dto.matching.MatchResult;
import com.worklink.entity.Category;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.FreelancerSkill;
import com.worklink.entity.Project;
import com.worklink.entity.ProjectSkill;
import com.worklink.entity.Skill;
import com.worklink.entity.User;
import com.worklink.entity.TrustHistory;
import com.worklink.entity.PerformanceHistory;
import com.worklink.entity.Proposal;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.ProjectComplexity;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.Role;
import com.worklink.entity.enums.SkillImportance;
import com.worklink.entity.enums.SkillProficiency;
import com.worklink.entity.enums.ProposalStatus;
import com.worklink.exception.ResourceNotFoundException;
import com.worklink.repository.CategoryRepository;
import com.worklink.repository.ClientProfileRepository;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.FreelancerSkillRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.ProjectSkillRepository;
import com.worklink.repository.SkillRepository;
import com.worklink.repository.UserRepository;
import com.worklink.repository.TrustHistoryRepository;
import com.worklink.repository.PerformanceHistoryRepository;
import com.worklink.repository.ProposalRepository;
import com.worklink.security.AuthenticatedUser;
import com.worklink.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class MatchingServiceTest {

    @Autowired private MatchingService matchingService;
    @Autowired private UserRepository userRepository;
    @Autowired private ClientProfileRepository clientProfileRepository;
    @Autowired private FreelancerProfileRepository freelancerProfileRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private ProjectSkillRepository projectSkillRepository;
    @Autowired private FreelancerSkillRepository freelancerSkillRepository;
    @Autowired private TrustHistoryRepository trustHistoryRepository;
    @Autowired private PerformanceHistoryRepository performanceHistoryRepository;
    @Autowired private ProposalRepository proposalRepository;

    @MockBean private CurrentUserProvider currentUserProvider;

    private User clientUser;
    private ClientProfile clientProfile;
    private Project project;
    private Skill skillJava;
    private Skill skillReact;

    @BeforeEach
    void setUp() {
        clientUser = userRepository.saveAndFlush(
                new User("Client", "Owner", "service.client@test.com", "hash", Role.CLIENT, AccountStatus.ACTIVE));
        clientProfile = clientProfileRepository.saveAndFlush(
                new ClientProfile(clientUser, "Owner Corp", "Desc", "Tech", "NYC", "https://owner.com"));

        Category category = categoryRepository.saveAndFlush(new Category("Software Dev", "Engineering"));
        skillJava = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> skillRepository.saveAndFlush(new Skill("Java", "Backend")));
        skillReact = skillRepository.findByNameIgnoreCase("React")
                .orElseGet(() -> skillRepository.saveAndFlush(new Skill("React", "Frontend")));

        project = projectRepository.saveAndFlush(new Project(
                clientProfile, "Full Stack Web App", "Build full stack app", category,
                new BigDecimal("3000"), new BigDecimal("6000"),
                3, LocalDate.now().plusDays(30), ProjectComplexity.MEDIUM, ProjectStatus.PUBLISHED));

        projectSkillRepository.saveAndFlush(new ProjectSkill(project, skillJava, SkillImportance.REQUIRED));
        projectSkillRepository.saveAndFlush(new ProjectSkill(project, skillReact, SkillImportance.PREFERRED));

        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(clientUser.getId(), clientUser.getEmail(), clientUser.getRole()));
    }

    @Test
    void matches_ranksByCompatibilityScoreDescendingWithSequentialRanks() {
        // High match freelancer: has expert skills, matching experience and budget
        FreelancerProfile highMatch = makeFreelancer("high.match@test.com", 5, new BigDecimal("4500"), AccountStatus.ACTIVE);
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(highMatch, skillJava, SkillProficiency.EXPERT));
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(highMatch, skillReact, SkillProficiency.EXPERT));

        // Medium match freelancer: has one skill
        FreelancerProfile midMatch = makeFreelancer("mid.match@test.com", 3, new BigDecimal("4000"), AccountStatus.ACTIVE);
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(midMatch, skillJava, SkillProficiency.INTERMEDIATE));

        // Low match freelancer: has no matching skills
        makeFreelancer("low.match@test.com", 0, new BigDecimal("10000"), AccountStatus.ACTIVE);

        List<MatchResult> results = matchingService.matches(project.getId(), 10);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).freelancerId()).isEqualTo(highMatch.getId());
        assertThat(results.get(0).rank()).isEqualTo(1);
        assertThat(results.get(0).matchedSkills()).contains("Java", "React");
        assertThat(results.get(0).trustAvailability()).isEqualTo("UNAVAILABLE");
        assertThat(results.get(0).trustScore()).isNull();
        assertThat(results.get(0).finalRankingScore()).isEqualTo(results.get(0).compatibilityScore());

        // Verify descending order
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).compatibilityScore())
                    .isGreaterThanOrEqualTo(results.get(i + 1).compatibilityScore());
            assertThat(results.get(i).rank()).isEqualTo(i + 1);
        }
    }

    @Test
    void matches_enforcesRequestedLimitAndClampsBetweenMinAndMax() {
        for (int i = 1; i <= 5; i++) {
            makeFreelancer("freelancer." + i + "@test.com", i, new BigDecimal("3500"), AccountStatus.ACTIVE);
        }

        List<MatchResult> limit2 = matchingService.matches(project.getId(), 2);
        assertThat(limit2).hasSize(2);
        assertThat(limit2.get(0).rank()).isEqualTo(1);
        assertThat(limit2.get(1).rank()).isEqualTo(2);

        List<MatchResult> limitNegative = matchingService.matches(project.getId(), -5);
        assertThat(limitNegative).hasSize(1); // Clamped to MIN_LIMIT = 1
    }

    @Test
    void matches_excludesInactiveFreelancers() {
        FreelancerProfile active = makeFreelancer("active.f@test.com", 4, new BigDecimal("4000"), AccountStatus.ACTIVE);
        FreelancerProfile inactive = makeFreelancer("inactive.f@test.com", 10, new BigDecimal("4000"), AccountStatus.INACTIVE);

        List<MatchResult> results = matchingService.matches(project.getId(), 50);

        assertThat(results).extracting(MatchResult::freelancerId)
                .contains(active.getId())
                .doesNotContain(inactive.getId());
    }

    @Test
    void matches_rejectsNonOwnerClient() {
        User otherClient = userRepository.saveAndFlush(
                new User("Other", "Client", "other.client@test.com", "hash", Role.CLIENT, AccountStatus.ACTIVE));
        clientProfileRepository.saveAndFlush(
                new ClientProfile(otherClient, "Other Corp", "Desc", "Tech", "LA", "https://other.com"));

        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(otherClient.getId(), otherClient.getEmail(), otherClient.getRole()));

        assertThatThrownBy(() -> matchingService.matches(project.getId(), 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found or access denied");
    }

    @Test
    void matches_rejectsDraftProject() {
        project.setStatus(ProjectStatus.DRAFT);
        projectRepository.saveAndFlush(project);

        assertThatThrownBy(() -> matchingService.matches(project.getId(), 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recommendations are available only for published projects");
    }

    @Test
    void matches_handlesNoEligibleFreelancersGracefully() {
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FREELANCER)
                .forEach(u -> {
                    u.setAccountStatus(AccountStatus.INACTIVE);
                    userRepository.save(u);
                });
        userRepository.flush();

        List<MatchResult> results = matchingService.matches(project.getId(), 10);
        assertThat(results).isEmpty();
    }

    @Test
    void matches_preservesAllMatchResultFields() {
        FreelancerProfile freelancer = makeFreelancer("full.fields@test.com", 4, new BigDecimal("4000"), AccountStatus.ACTIVE);
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(freelancer, skillJava, SkillProficiency.ADVANCED));

        List<MatchResult> results = matchingService.matches(project.getId(), 10);
        MatchResult match = results.stream()
                .filter(r -> r.freelancerId().equals(freelancer.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(match.rank()).isPositive();
        assertThat(match.freelancerId()).isEqualTo(freelancer.getId());
        assertThat(match.freelancerName()).isNotEmpty();
        assertThat(match.freelancerHeadline()).isNotEmpty();
        assertThat(match.freelancerExperienceYears()).isEqualTo(4);
        assertThat(match.compatibilityScore()).isBetween(0.0, 100.0);
        assertThat(match.skillMatchScore()).isBetween(0.0, 100.0);
        assertThat(match.proficiencyScore()).isBetween(0.0, 100.0);
        assertThat(match.experienceScore()).isBetween(0.0, 100.0);
        assertThat(match.portfolioRelevanceScore()).isBetween(0.0, 100.0);
        assertThat(match.budgetCompatibilityScore()).isBetween(0.0, 100.0);
        assertThat(match.matchedSkills()).contains("Java");
        assertThat(match.missingSkills()).contains("React");
        assertThat(match.explanation()).isNotEmpty();
        assertThat(match.trustAvailability()).isEqualTo("UNAVAILABLE");
        assertThat(match.finalRankingScore()).isEqualTo(match.compatibilityScore());
    }

    @Test
    void trustSnapshotIsCombinedSeparatelyFromCompatibility() {
        FreelancerProfile freelancer=makeFreelancer("trusted.match@test.com",4,new BigDecimal("4000"),AccountStatus.ACTIVE);
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(freelancer,skillJava,SkillProficiency.ADVANCED));
        saveTrust(freelancer,20.0,LocalDateTime.now().minusDays(1));
        saveTrust(freelancer,78.0,LocalDateTime.now());

        MatchResult match=matchingService.matches(project.getId(),10).stream()
                .filter(result->result.freelancerId().equals(freelancer.getId())).findFirst().orElseThrow();

        assertThat(match.trustAvailability()).isEqualTo("AVAILABLE");
        assertThat(match.trustScore()).isEqualTo(78.0);
        assertThat(match.finalRankingScore()).isEqualTo(round(match.compatibilityScore()*0.70+78.0*0.30));
        assertThat(match.semanticAvailability()).isEqualTo("AVAILABLE");
        assertThat(match.semanticSimilarity()).isNotNull();
        assertThat(match.semanticRelevanceScore()).isBetween(0.0, 100.0);
        assertThat(match.rankingExplanation()).contains("0.70", "0.30");
        assertThat(match.explanation()).isNotEmpty();
    }

    @Test
    void higherTrustCanMoveLowerCompatibilityFreelancerAhead() {
        FreelancerProfile highCompatibility=makeFreelancer("high.compatibility@test.com",5,new BigDecimal("4500"),AccountStatus.ACTIVE);
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(highCompatibility,skillJava,SkillProficiency.EXPERT));
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(highCompatibility,skillReact,SkillProficiency.EXPERT));
        FreelancerProfile lowerCompatibility=makeFreelancer("lower.compatibility@test.com",3,new BigDecimal("4000"),AccountStatus.ACTIVE);
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(lowerCompatibility,skillJava,SkillProficiency.INTERMEDIATE));
        saveTrust(highCompatibility,0.0,LocalDateTime.now()); saveTrust(lowerCompatibility,100.0,LocalDateTime.now());

        List<MatchResult> ranked=matchingService.matches(project.getId(),10);
        MatchResult first=ranked.get(0);
        assertThat(first.freelancerId()).isEqualTo(lowerCompatibility.getId());
        MatchResult high=ranked.stream().filter(r->r.freelancerId().equals(highCompatibility.getId())).findFirst().orElseThrow();
        assertThat(high.compatibilityScore()).isGreaterThan(first.compatibilityScore());
        assertThat(first.finalRankingScore()).isGreaterThan(high.finalRankingScore());
    }

    @Test
    void equalScoresUseFreelancerIdForDeterministicTieBreak() {
        FreelancerProfile first=makeFreelancer("tie.first@test.com",4,new BigDecimal("4000"),AccountStatus.ACTIVE);
        FreelancerProfile second=makeFreelancer("tie.second@test.com",4,new BigDecimal("4000"),AccountStatus.ACTIVE);
        saveTrust(first,75.0,LocalDateTime.now()); saveTrust(second,75.0,LocalDateTime.now());
        List<Long> tied=matchingService.matches(project.getId(),10).stream()
                .filter(r->r.freelancerId().equals(first.getId())||r.freelancerId().equals(second.getId()))
                .map(MatchResult::freelancerId).toList();
        assertThat(tied).containsExactly(first.getId(),second.getId());
    }

    @Test
    void contextAwareScoreUsesAvailableSignalsAndLeavesBaselineUnchanged() {
        FreelancerProfile freelancer = makeFreelancer("context.match@test.com", 4,
                new BigDecimal("4000"), AccountStatus.ACTIVE);
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(freelancer, skillJava, SkillProficiency.ADVANCED));
        saveTrust(freelancer, 80.0, LocalDateTime.now());

        MatchResult baseline = matchingService.matches(project.getId(), 10).stream()
                .filter(match -> match.freelancerId().equals(freelancer.getId())).findFirst().orElseThrow();
        var context = matchingService.contextAwareMatches(project.getId(), 10).stream()
                .filter(match -> match.freelancerId().equals(freelancer.getId())).findFirst().orElseThrow();

        // Performance is absent, so the configured 50/20/20 available weights are renormalized to 5/9, 2/9, 2/9.
        double expected = (baseline.compatibilityScore() * 0.50
                + context.semanticRelevanceScore() * 0.20 + 80.0 * 0.20) / 0.90;
        assertThat(context.advancedMatchScore()).isEqualTo(round(expected));
        assertThat(context.performanceAvailability()).isEqualTo("UNAVAILABLE");
        assertThat(context.signalContributions()).filteredOn(c -> c.signal().equals("performance"))
                .singleElement().satisfies(c -> assertThat(c.effectiveWeight()).isNull());
        assertThat(context.model()).isEqualTo("worklink_context_aware");
        assertThat(matchingService.matches(project.getId(), 10).stream()
                .filter(match -> match.freelancerId().equals(freelancer.getId())).findFirst().orElseThrow())
                .isEqualTo(baseline);
    }

    @Test
    void contextAwareScoreCombinesAllFourPersistedOrComputedSignals() {
        FreelancerProfile freelancer = makeFreelancer("context.complete@test.com", 4,
                new BigDecimal("4000"), AccountStatus.ACTIVE);
        freelancerSkillRepository.saveAndFlush(new FreelancerSkill(freelancer, skillJava, SkillProficiency.ADVANCED));
        saveTrust(freelancer, 80.0, LocalDateTime.now());

        Project priorProject = projectRepository.saveAndFlush(new Project(clientProfile, "Prior project", "Completed work",
                project.getCategory(), new BigDecimal("1000"), new BigDecimal("2000"), 1,
                LocalDate.now().minusDays(2), ProjectComplexity.LOW, ProjectStatus.COMPLETED));
        Proposal accepted = proposalRepository.saveAndFlush(new Proposal(priorProject, freelancer, "Completed work",
                new BigDecimal("1500"), 2, ProposalStatus.ACCEPTED, null));
        PerformanceHistory outcome = new PerformanceHistory();
        outcome.setFreelancer(freelancer); outcome.setProject(priorProject); outcome.setAcceptedProposal(accepted);
        outcome.setProjectStatus(ProjectStatus.COMPLETED); outcome.setCompletionDate(LocalDate.now().minusDays(1));
        outcome.setDeadline(LocalDate.now().minusDays(2)); outcome.setOnTime(false);
        outcome.setMilestoneCount(2); outcome.setApprovedMilestoneCount(1); outcome.setClientRating(5);
        performanceHistoryRepository.saveAndFlush(outcome);

        MatchResult baseline = matchingService.matches(project.getId(), 10).stream()
                .filter(match -> match.freelancerId().equals(freelancer.getId())).findFirst().orElseThrow();
        var context = matchingService.contextAwareMatches(project.getId(), 10).stream()
                .filter(match -> match.freelancerId().equals(freelancer.getId())).findFirst().orElseThrow();

        assertThat(context.semanticAvailability()).isEqualTo("AVAILABLE");
        assertThat(context.trustAvailability()).isEqualTo("AVAILABLE");
        assertThat(context.performanceAvailability()).isEqualTo("AVAILABLE");
        assertThat(context.performanceScore()).isEqualTo(62.5); // mean(100 completion, 0 on-time, 50 milestones, 100 rating)
        assertThat(context.advancedMatchScore()).isEqualTo(round(baseline.compatibilityScore() * 0.50
                + context.semanticRelevanceScore() * 0.20 + 80.0 * 0.20 + 62.5 * 0.10));
        var effectiveWeights = context.signalContributions().stream().map(c -> c.effectiveWeight()).toList();
        assertThat(effectiveWeights).hasSize(4);
        assertThat(effectiveWeights.get(0)).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(effectiveWeights.get(1)).isCloseTo(0.2, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(effectiveWeights.get(2)).isCloseTo(0.2, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(effectiveWeights.get(3)).isCloseTo(0.1, org.assertj.core.data.Offset.offset(0.000001));
    }

    @Test
    void contextAwareMatchWithOnlyCompatibilityAvailableUsesItWithoutPenalty() {
        User user = userRepository.saveAndFlush(new User("Cold", "Start", "context.cold@test.com", "hash",
                Role.FREELANCER, AccountStatus.ACTIVE));
        FreelancerProfile freelancer = freelancerProfileRepository.saveAndFlush(
                new FreelancerProfile(user, null, null, "Remote", 0, null, "AVAILABLE"));

        var result = matchingService.contextAwareMatches(project.getId(), 10).stream()
                .filter(match -> match.freelancerId().equals(freelancer.getId())).findFirst().orElseThrow();

        assertThat(result.semanticAvailability()).isEqualTo("UNAVAILABLE");
        assertThat(result.trustAvailability()).isEqualTo("UNAVAILABLE");
        assertThat(result.performanceAvailability()).isEqualTo("UNAVAILABLE");
        assertThat(result.advancedMatchScore()).isEqualTo(result.compatibilityScore());
        assertThat(result.signalContributions()).filteredOn(c -> c.signal().equals("compatibility"))
                .singleElement().satisfies(c -> assertThat(c.effectiveWeight()).isEqualTo(1.0));
    }

    @Test
    void contextAwareOrderingUsesAdvancedScoreAndDeterministicFreelancerIdTieBreak() {
        FreelancerProfile lowerId = makeFreelancer("context.tie.one@test.com", 3,
                new BigDecimal("4000"), AccountStatus.ACTIVE);
        FreelancerProfile higherId = makeFreelancer("context.tie.two@test.com", 3,
                new BigDecimal("4000"), AccountStatus.ACTIVE);
        var tied = matchingService.contextAwareMatches(project.getId(), 50).stream()
                .filter(result -> result.freelancerId().equals(lowerId.getId())
                        || result.freelancerId().equals(higherId.getId())).toList();
        assertThat(tied).extracting(result -> result.freelancerId())
                .containsExactly(lowerId.getId(), higherId.getId());
        assertThat(tied.get(0).rank()).isLessThan(tied.get(1).rank());

        FreelancerProfile trusted = makeFreelancer("context.trusted@test.com", 3,
                new BigDecimal("4000"), AccountStatus.ACTIVE);
        saveTrust(trusted, 100.0, LocalDateTime.now());
        var ranked = matchingService.contextAwareMatches(project.getId(), 50);
        assertThat(ranked).isSortedAccordingTo(java.util.Comparator.comparingDouble(
                com.worklink.dto.matching.ContextAwareMatchResult::advancedMatchScore).reversed());
        assertThat(ranked.indexOf(ranked.stream().filter(result -> result.freelancerId().equals(trusted.getId())).findFirst().orElseThrow()))
                .isLessThan(ranked.indexOf(ranked.stream().filter(result -> result.freelancerId().equals(lowerId.getId())).findFirst().orElseThrow()));
    }

    private void saveTrust(FreelancerProfile freelancer,double score,LocalDateTime at) {
        TrustHistory trust=new TrustHistory(); trust.setFreelancer(freelancer); trust.setOverallTrustScore(score);
        trust.setProjectsConsidered(1); trust.setCalculatedAt(at); trustHistoryRepository.saveAndFlush(trust);
    }

    private double round(double value){return Math.round(value*100.0)/100.0;}

    private FreelancerProfile makeFreelancer(String email, int years, BigDecimal rate, AccountStatus status) {
        User u = userRepository.saveAndFlush(
                new User("Dev", email.split("@")[0], email, "hash", Role.FREELANCER, status));
        return freelancerProfileRepository.saveAndFlush(
                new FreelancerProfile(u, "Senior Developer", "Bio details", "Remote", years, rate, "AVAILABLE"));
    }
}
