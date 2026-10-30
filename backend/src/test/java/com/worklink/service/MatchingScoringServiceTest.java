package com.worklink.service;

import com.worklink.dto.matching.MatchResult;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.FreelancerSkill;
import com.worklink.entity.PortfolioProject;
import com.worklink.entity.Project;
import com.worklink.entity.ProjectSkill;
import com.worklink.entity.Proposal;
import com.worklink.entity.Skill;
import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.ProjectComplexity;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.ProposalStatus;
import com.worklink.entity.enums.Role;
import com.worklink.entity.enums.SkillImportance;
import com.worklink.entity.enums.SkillProficiency;
import com.worklink.repository.ClientProfileRepository;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.FreelancerSkillRepository;
import com.worklink.repository.PortfolioProjectRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.ProjectSkillRepository;
import com.worklink.repository.ProposalRepository;
import com.worklink.repository.SkillRepository;
import com.worklink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Block 4.2 — Matching Engine Scoring Tests.
 *
 * All tests use controlled data so expected scores can be computed manually.
 * Tests cover every scoring component individually and in combination.
 */
@SpringBootTest
@Transactional
class MatchingScoringServiceTest {

    @Autowired MatchingScoringService scoringService;

    @Autowired UserRepository userRepository;
    @Autowired ClientProfileRepository clientProfileRepository;
    @Autowired FreelancerProfileRepository freelancerProfileRepository;
    @Autowired SkillRepository skillRepository;
    @Autowired FreelancerSkillRepository freelancerSkillRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectSkillRepository projectSkillRepository;
    @Autowired PortfolioProjectRepository portfolioProjectRepository;
    @Autowired ProposalRepository proposalRepository;

    // Shared test fixtures
    private Skill skillJava;
    private Skill skillReact;
    private Skill skillDocker;
    private ClientProfile client;
    private Project project;

    @BeforeEach
    void setUp() {
        // Client
        User clientUser = userRepository.saveAndFlush(
                new User("Test", "Client", "client.match@test.com", "hash", Role.CLIENT, AccountStatus.ACTIVE));
        client = clientProfileRepository.saveAndFlush(
                new ClientProfile(clientUser, "Test Corp", "desc", "Tech", "NYC", "https://test.com"));

        // Skills
        skillJava   = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> skillRepository.saveAndFlush(new Skill("Java", "Backend")));
        skillReact  = skillRepository.findByNameIgnoreCase("React")
                .orElseGet(() -> skillRepository.saveAndFlush(new Skill("React", "Frontend")));
        skillDocker = skillRepository.findByNameIgnoreCase("Docker")
                .orElseGet(() -> skillRepository.saveAndFlush(new Skill("Docker", "DevOps")));

        // Project with 3 skills: Java=REQUIRED, React=IMPORTANT, Docker=PREFERRED
        project = projectRepository.saveAndFlush(new Project(
                client, "Backend Platform", "Build microservices", null,
                new BigDecimal("3000"), new BigDecimal("6000"),
                3, LocalDate.now().plusMonths(3), ProjectComplexity.HIGH, ProjectStatus.PUBLISHED));

        projectSkillRepository.saveAndFlush(new ProjectSkill(project, skillJava,   SkillImportance.REQUIRED));
        projectSkillRepository.saveAndFlush(new ProjectSkill(project, skillReact,  SkillImportance.IMPORTANT));
        projectSkillRepository.saveAndFlush(new ProjectSkill(project, skillDocker, SkillImportance.PREFERRED));
    }

    // =========================================================================
    // A. SKILL MATCH TESTS
    // =========================================================================

    @Test
    void skillMatch_allRequiredSkillsMatched_gives100() {
        FreelancerProfile f = makeFreelancer("all@match.com", 5, new BigDecimal("80"));
        addSkill(f, skillJava,   SkillProficiency.EXPERT);
        addSkill(f, skillReact,  SkillProficiency.ADVANCED);
        addSkill(f, skillDocker, SkillProficiency.INTERMEDIATE);

        MatchResult result = scoringService.score(project, f);

        // Total weight = 1.0 + 0.7 + 0.4 = 2.1, matched = 2.1 → score = 100
        assertThat(result.skillMatchScore()).isCloseTo(100.0, within(0.1));
        assertThat(result.matchedSkills()).containsExactlyInAnyOrder("Java", "React", "Docker");
        assertThat(result.missingSkills()).isEmpty();
    }

    @Test
    void skillMatch_onlyRequiredSkillMatched_correctPartialScore() {
        FreelancerProfile f = makeFreelancer("partial@match.com", 5, new BigDecimal("80"));
        addSkill(f, skillJava, SkillProficiency.EXPERT); // REQUIRED weight=1.0 only

        MatchResult result = scoringService.score(project, f);

        // matched weight = 1.0, total = 2.1 → 100 * 1.0/2.1 ≈ 47.62
        assertThat(result.skillMatchScore()).isCloseTo(47.62, within(1.0));
        assertThat(result.matchedSkills()).containsExactly("Java");
        assertThat(result.missingSkills()).containsExactlyInAnyOrder("React", "Docker");
    }

    @Test
    void skillMatch_onlyPreferredSkillMatched_lowScore() {
        FreelancerProfile f = makeFreelancer("preferred@match.com", 5, new BigDecimal("80"));
        addSkill(f, skillDocker, SkillProficiency.EXPERT); // PREFERRED weight=0.4

        MatchResult result = scoringService.score(project, f);

        // matched weight = 0.4, total = 2.1 → 100 * 0.4/2.1 ≈ 19.05
        assertThat(result.skillMatchScore()).isCloseTo(19.05, within(1.0));
    }

    @Test
    void skillMatch_onlyImportantSkillMatched_correctScore() {
        FreelancerProfile f = makeFreelancer("important@match.com", 5, new BigDecimal("80"));
        addSkill(f, skillReact, SkillProficiency.ADVANCED); // IMPORTANT weight=0.7

        MatchResult result = scoringService.score(project, f);

        // matched weight = 0.7, total = 2.1 → 100 * 0.7/2.1 ≈ 33.33
        assertThat(result.skillMatchScore()).isCloseTo(33.33, within(1.0));
    }

    @Test
    void skillMatch_noSkillsMatched_givesZero() {
        FreelancerProfile f = makeFreelancer("none@match.com", 5, new BigDecimal("80"));
        // No skills added

        MatchResult result = scoringService.score(project, f);

        assertThat(result.skillMatchScore()).isCloseTo(0.0, within(0.1));
        assertThat(result.matchedSkills()).isEmpty();
        assertThat(result.missingSkills()).containsExactlyInAnyOrder("Java", "React", "Docker");
    }

    // =========================================================================
    // B. PROFICIENCY TESTS
    // =========================================================================

    @Test
    void proficiency_expertOnRequiredSkill_highScore() {
        FreelancerProfile f = makeFreelancer("expert@match.com", 5, new BigDecimal("80"));
        addSkill(f, skillJava, SkillProficiency.EXPERT); // weight=1.0, value=100

        MatchResult result = scoringService.score(project, f);

        // Only Java matched: proficiency = 100 * 1.0 / 1.0 = 100
        assertThat(result.proficiencyScore()).isCloseTo(100.0, within(0.1));
    }

    @Test
    void proficiency_advancedOnRequiredSkill_score75() {
        FreelancerProfile f = makeFreelancer("advanced@match.com", 5, new BigDecimal("80"));
        addSkill(f, skillJava, SkillProficiency.ADVANCED); // value=75

        MatchResult result = scoringService.score(project, f);
        assertThat(result.proficiencyScore()).isCloseTo(75.0, within(0.1));
    }

    @Test
    void proficiency_intermediateOnRequiredSkill_score50() {
        FreelancerProfile f = makeFreelancer("inter@match.com", 5, new BigDecimal("80"));
        addSkill(f, skillJava, SkillProficiency.INTERMEDIATE); // value=50

        MatchResult result = scoringService.score(project, f);
        assertThat(result.proficiencyScore()).isCloseTo(50.0, within(0.1));
    }

    @Test
    void proficiency_beginnerOnRequiredSkill_score25() {
        FreelancerProfile f = makeFreelancer("beginner@match.com", 5, new BigDecimal("80"));
        addSkill(f, skillJava, SkillProficiency.BEGINNER); // value=25

        MatchResult result = scoringService.score(project, f);
        assertThat(result.proficiencyScore()).isCloseTo(25.0, within(0.1));
    }

    @Test
    void proficiency_unownedSkillGivesNoProficiencyCredit() {
        FreelancerProfile f = makeFreelancer("noskill.prof@match.com", 5, new BigDecimal("80"));
        // No skills — proficiency must be 0

        MatchResult result = scoringService.score(project, f);
        assertThat(result.proficiencyScore()).isCloseTo(0.0, within(0.1));
    }

    // =========================================================================
    // C. EXPERIENCE TESTS
    // =========================================================================

    @Test
    void experience_meetsRequirement_score100() {
        FreelancerProfile f = makeFreelancer("exp.meets@match.com", 3, new BigDecimal("80")); // required=3
        MatchResult result = scoringService.score(project, f);
        assertThat(result.experienceScore()).isCloseTo(100.0, within(0.1));
    }

    @Test
    void experience_exceedsRequirement_cappedAt100() {
        FreelancerProfile f = makeFreelancer("exp.exceeds@match.com", 10, new BigDecimal("80")); // required=3
        MatchResult result = scoringService.score(project, f);
        assertThat(result.experienceScore()).isCloseTo(100.0, within(0.1));
    }

    @Test
    void experience_belowRequirement_proportionalScore() {
        FreelancerProfile f = makeFreelancer("exp.below@match.com", 1, new BigDecimal("80")); // required=3
        MatchResult result = scoringService.score(project, f);
        // 100 * 1/3 ≈ 33.33
        assertThat(result.experienceScore()).isCloseTo(33.33, within(1.0));
    }

    @Test
    void experience_zeroRequired_score100Regardless() {
        // Project with zero required experience
        Project zeroExpProject = projectRepository.saveAndFlush(new Project(
                client, "Zero Exp", "Any experience", null,
                new BigDecimal("1000"), new BigDecimal("2000"),
                0, LocalDate.now().plusMonths(1), ProjectComplexity.LOW, ProjectStatus.PUBLISHED));

        FreelancerProfile f = makeFreelancer("zero.exp@match.com", 0, new BigDecimal("50"));
        MatchResult result = scoringService.score(zeroExpProject, f);
        assertThat(result.experienceScore()).isCloseTo(100.0, within(0.1));
    }

    @Test
    void experience_nullValues_handledSafely() {
        FreelancerProfile f = makeFreelancer("null.exp@match.com", 0, new BigDecimal("80"));
        f.setYearsOfExperience(null);
        freelancerProfileRepository.saveAndFlush(f);

        // Should not throw — yearsOfExperience null → treated as 0
        MatchResult result = scoringService.score(project, f);
        assertThat(result.experienceScore()).isGreaterThanOrEqualTo(0.0);
    }

    // =========================================================================
    // D. PORTFOLIO RELEVANCE TESTS
    // =========================================================================

    @Test
    void portfolio_highlyRelevant_highScore() {
        FreelancerProfile f = makeFreelancer("portfolio.high@match.com", 5, new BigDecimal("80"));
        // Portfolio mentions all three project skills
        addPortfolio(f, "Java Microservices", "Built React frontend with Docker", "Java, React, Docker");

        MatchResult result = scoringService.score(project, f);
        // All 3 skills covered → score = 100 * 2.1/2.1 = 100
        assertThat(result.portfolioRelevanceScore()).isCloseTo(100.0, within(1.0));
    }

    @Test
    void portfolio_partiallyRelevant_partialScore() {
        FreelancerProfile f = makeFreelancer("portfolio.partial@match.com", 5, new BigDecimal("80"));
        // Only Java mentioned (REQUIRED, weight=1.0)
        addPortfolio(f, "Java API", "Backend Java service", "Java");

        MatchResult result = scoringService.score(project, f);
        // covered weight = 1.0 (Java), total = 2.1 → 100 * 1.0/2.1 ≈ 47.62
        assertThat(result.portfolioRelevanceScore()).isCloseTo(47.62, within(2.0));
    }

    @Test
    void portfolio_unrelated_zeroScore() {
        FreelancerProfile f = makeFreelancer("portfolio.unrelated@match.com", 5, new BigDecimal("80"));
        addPortfolio(f, "Graphic Design", "Created logos and branding", "Photoshop, Illustrator");

        MatchResult result = scoringService.score(project, f);
        assertThat(result.portfolioRelevanceScore()).isCloseTo(0.0, within(0.1));
    }

    @Test
    void portfolio_empty_zeroScore() {
        FreelancerProfile f = makeFreelancer("portfolio.empty@match.com", 5, new BigDecimal("80"));
        // No portfolio entries

        MatchResult result = scoringService.score(project, f);
        assertThat(result.portfolioRelevanceScore()).isCloseTo(0.0, within(0.1));
    }

    // =========================================================================
    // E. BUDGET COMPATIBILITY TESTS
    // =========================================================================

    @Test
    void budget_withinRange_score100() {
        // Project budget: 3000-6000. Freelancer rate=4500 (within range)
        FreelancerProfile f = makeFreelancer("budget.close@match.com", 5, new BigDecimal("4500"));

        MatchResult result = scoringService.score(project, f);
        assertThat(result.budgetCompatibilityScore()).isCloseTo(100.0, within(0.1));
    }

    @Test
    void budget_atMax_score100() {
        FreelancerProfile f = makeFreelancer("budget.atmax@match.com", 5, new BigDecimal("6000"));
        MatchResult result = scoringService.score(project, f);
        assertThat(result.budgetCompatibilityScore()).isCloseTo(100.0, within(0.1));
    }

    @Test
    void budget_moderatlyOver_reducedScore() {
        // rate=9000, max=6000 → excess=3000, penalty=100*3000/6000=50 → score=50
        FreelancerProfile f = makeFreelancer("budget.moderate@match.com", 5, new BigDecimal("9000"));
        MatchResult result = scoringService.score(project, f);
        assertThat(result.budgetCompatibilityScore()).isCloseTo(50.0, within(1.0));
    }

    @Test
    void budget_largelyOver_veryLowScore() {
        // rate=18000, max=6000 → excess=12000, penalty=100*12000/6000=200 → capped at 0
        FreelancerProfile f = makeFreelancer("budget.large@match.com", 5, new BigDecimal("18000"));
        MatchResult result = scoringService.score(project, f);
        assertThat(result.budgetCompatibilityScore()).isCloseTo(0.0, within(1.0));
    }

    @Test
    void budget_nullRate_neutralScore50() {
        FreelancerProfile f = makeFreelancer("budget.null@match.com", 5, null);
        MatchResult result = scoringService.score(project, f);
        assertThat(result.budgetCompatibilityScore()).isCloseTo(50.0, within(0.1));
    }

    @Test
    void budget_proposalBudgetUsedWhenExists() {
        FreelancerProfile f = makeFreelancer("budget.proposal@match.com", 5, new BigDecimal("99999"));
        // Freelancer rate is way over budget, but they submitted a proposal for 5000 (within range)
        proposalRepository.saveAndFlush(new Proposal(
                project, f, "My cover letter", new BigDecimal("5000"), 30, ProposalStatus.SUBMITTED, null));

        MatchResult result = scoringService.score(project, f);
        // Proposal budget 5000 is within 3000-6000 → score should be 100
        assertThat(result.budgetCompatibilityScore()).isCloseTo(100.0, within(0.1));
    }

    @Test
    void budget_hourlyRateUsedWhenNoProposal() {
        // Within budget: 4000 between 3000-6000
        FreelancerProfile f = makeFreelancer("budget.hourly@match.com", 5, new BigDecimal("4000"));
        // No proposal exists

        MatchResult result = scoringService.score(project, f);
        assertThat(result.budgetCompatibilityScore()).isCloseTo(100.0, within(0.1));
    }

    // =========================================================================
    // F. OVERALL WEIGHTED SCORE VERIFICATION
    // =========================================================================

    @Test
    void overallScore_verifyWeightedCombination() {
        // Freelancer: all 3 skills + expert on Java, React + 3 years exp + full portfolio + budget 5000
        FreelancerProfile f = makeFreelancer("overall@match.com", 3, new BigDecimal("5000"));
        addSkill(f, skillJava,   SkillProficiency.EXPERT);   // REQUIRED, prof=100
        addSkill(f, skillReact,  SkillProficiency.EXPERT);   // IMPORTANT, prof=100
        addSkill(f, skillDocker, SkillProficiency.EXPERT);   // PREFERRED, prof=100
        addPortfolio(f, "Full Stack Java React Docker App", "React frontend, Java backend, Docker", "Java, React, Docker");

        MatchResult result = scoringService.score(project, f);

        // skill     = 100 (all matched)
        // proficiency= 100 (all expert)
        // experience = 100 (3 years == 3 required)
        // portfolio  = 100 (all skills mentioned)
        // budget     = 100 (5000 within 3000-6000)
        // final = 0.40*100 + 0.20*100 + 0.15*100 + 0.10*100 + 0.15*100 = 100
        assertThat(result.compatibilityScore()).isCloseTo(100.0, within(1.0));
        assertThat(result.skillMatchScore()).isCloseTo(100.0, within(0.1));
        assertThat(result.proficiencyScore()).isCloseTo(100.0, within(0.1));
        assertThat(result.experienceScore()).isCloseTo(100.0, within(0.1));
        assertThat(result.portfolioRelevanceScore()).isCloseTo(100.0, within(1.0));
        assertThat(result.budgetCompatibilityScore()).isCloseTo(100.0, within(0.1));
    }

    @Test
    void overallScore_partialMatchVerifyWeights() {
        // Only Java (REQUIRED), ADVANCED, 1 year exp, no portfolio, budget null
        FreelancerProfile f = makeFreelancer("partial.overall@match.com", 1, null);
        addSkill(f, skillJava, SkillProficiency.ADVANCED);

        MatchResult result = scoringService.score(project, f);

        // skill     = 100 * 1.0 / 2.1 ≈ 47.62
        // proficiency = 75 (ADVANCED, only Java matched, weight=1.0/1.0=75)
        // experience = 100 * 1/3 ≈ 33.33
        // portfolio  = 0
        // budget     = 50 (null)
        double expectedSkill      = 100.0 * 1.0 / 2.1;
        double expectedProf       = 75.0;
        double expectedExp        = 100.0 * 1.0 / 3.0;
        double expectedPortfolio  = 0.0;
        double expectedBudget     = 50.0;
        double expectedFinal = 0.40 * expectedSkill
                             + 0.20 * expectedProf
                             + 0.15 * expectedExp
                             + 0.10 * expectedPortfolio
                             + 0.15 * expectedBudget;

        assertThat(result.compatibilityScore()).isCloseTo(expectedFinal, within(1.5));
    }

    // =========================================================================
    // G. MATCH RESULT STRUCTURE
    // =========================================================================

    @Test
    void matchResult_containsAllRequiredFields() {
        FreelancerProfile f = makeFreelancer("struct@match.com", 4, new BigDecimal("5000"));
        addSkill(f, skillJava, SkillProficiency.ADVANCED);

        MatchResult result = scoringService.score(project, f);

        assertThat(result.freelancerId()).isNotNull();
        assertThat(result.freelancerName()).isNotBlank();
        assertThat(result.compatibilityScore()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
        assertThat(result.skillMatchScore()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
        assertThat(result.proficiencyScore()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
        assertThat(result.experienceScore()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
        assertThat(result.portfolioRelevanceScore()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
        assertThat(result.budgetCompatibilityScore()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
        assertThat(result.matchedSkills()).isNotNull();
        assertThat(result.missingSkills()).isNotNull();
        assertThat(result.explanation()).isNotBlank();
    }

    @Test
    void matchResult_explanation_reflectsActualScores() {
        // Full match
        FreelancerProfile f = makeFreelancer("explain.full@match.com", 5, new BigDecimal("5000"));
        addSkill(f, skillJava,   SkillProficiency.EXPERT);
        addSkill(f, skillReact,  SkillProficiency.EXPERT);
        addSkill(f, skillDocker, SkillProficiency.EXPERT);

        MatchResult full = scoringService.score(project, f);
        assertThat(full.explanation()).contains("3/3");

        // Partial match
        FreelancerProfile f2 = makeFreelancer("explain.partial@match.com", 1, null);
        addSkill(f2, skillJava, SkillProficiency.BEGINNER);
        MatchResult partial = scoringService.score(project, f2);
        assertThat(partial.explanation()).contains("1/3");

        // No match
        FreelancerProfile f3 = makeFreelancer("explain.none@match.com", 0, null);
        MatchResult none = scoringService.score(project, f3);
        assertThat(none.explanation()).contains("0/3");
    }

    // =========================================================================
    // H. RANKING — higher scores appear first
    // =========================================================================

    @Test
    void ranking_higherCompatibilityFreelancerRanksFirst() {
        FreelancerProfile high = makeFreelancer("rank.high@match.com", 5, new BigDecimal("5000"));
        addSkill(high, skillJava,   SkillProficiency.EXPERT);
        addSkill(high, skillReact,  SkillProficiency.EXPERT);
        addSkill(high, skillDocker, SkillProficiency.EXPERT);

        FreelancerProfile low = makeFreelancer("rank.low@match.com", 0, null);
        // No skills added

        MatchResult highResult = scoringService.score(project, high);
        MatchResult lowResult  = scoringService.score(project, low);

        assertThat(highResult.compatibilityScore()).isGreaterThan(lowResult.compatibilityScore());
    }

    // =========================================================================
    // I. LIMIT ENFORCEMENT
    // =========================================================================

    @Test
    void matchingService_limit_clampsToMax() {
        assertThat(MatchingService.MAX_LIMIT).isEqualTo(50);
        assertThat(MatchingService.MIN_LIMIT).isEqualTo(1);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private FreelancerProfile makeFreelancer(String email, int years, BigDecimal rate) {
        User u = userRepository.saveAndFlush(
                new User("Dev", email.split("@")[0], email, "hash", Role.FREELANCER, AccountStatus.ACTIVE));
        return freelancerProfileRepository.saveAndFlush(
                new FreelancerProfile(u, "Developer", "Bio", "Remote", years, rate, "AVAILABLE"));
    }

    private FreelancerSkill addSkill(FreelancerProfile f, Skill skill, SkillProficiency prof) {
        return freelancerSkillRepository.saveAndFlush(new FreelancerSkill(f, skill, prof));
    }

    private PortfolioProject addPortfolio(FreelancerProfile f, String title, String desc, String tech) {
        return portfolioProjectRepository.saveAndFlush(
                new PortfolioProject(f, title, desc, null, tech, null));
    }
}
