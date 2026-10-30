package com.worklink.repository;

import com.worklink.entity.Category;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.FreelancerSkill;
import com.worklink.entity.Project;
import com.worklink.entity.ProjectSkill;
import com.worklink.entity.Proposal;
import com.worklink.entity.MatchingEvaluationRecord;
import com.worklink.entity.TrustHistory;
import com.worklink.entity.Skill;
import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.ProjectComplexity;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.ProposalStatus;
import com.worklink.entity.enums.Role;
import com.worklink.entity.enums.SkillImportance;
import com.worklink.entity.enums.SkillProficiency;
import com.worklink.entity.enums.MatchingOutcomeLabel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class JpaEntityPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientProfileRepository clientProfileRepository;

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private FreelancerSkillRepository freelancerSkillRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectSkillRepository projectSkillRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private TrustHistoryRepository trustHistoryRepository;

    @Autowired
    private MatchingEvaluationRecordRepository matchingEvaluationRecordRepository;

    @Test
    void shouldPersistMatchingEvaluationProjectFreelancerAndAcceptedProposalRelationships() {
        User clientUser = userRepository.saveAndFlush(new User("Eval", "Client", "eval-client@example.com", "hash", Role.CLIENT, AccountStatus.ACTIVE));
        ClientProfile client = clientProfileRepository.saveAndFlush(new ClientProfile(clientUser, "Eval Corp", "desc", "Tech", "Remote", null));
        User freelancerUser = userRepository.saveAndFlush(new User("Eval", "Freelancer", "eval-freelancer@example.com", "hash", Role.FREELANCER, AccountStatus.ACTIVE));
        FreelancerProfile freelancer = freelancerProfileRepository.saveAndFlush(new FreelancerProfile(freelancerUser, "Builder", "Bio", "Remote", 4, new BigDecimal("40"), "AVAILABLE"));
        Project project = projectRepository.saveAndFlush(new Project(client, "Observed delivery", "Real completed project", null,
                new BigDecimal("100"), new BigDecimal("200"), 2, LocalDate.now().plusDays(10), ProjectComplexity.MEDIUM, ProjectStatus.COMPLETED));
        project.setAcceptedFreelancer(freelancer);
        projectRepository.saveAndFlush(project);
        Proposal proposal = proposalRepository.saveAndFlush(new Proposal(project, freelancer, "Accepted bid", new BigDecimal("150"), 7, ProposalStatus.ACCEPTED, null));

        MatchingEvaluationRecord record = new MatchingEvaluationRecord();
        record.setProject(project); record.setFreelancer(freelancer); record.setAcceptedProposal(proposal);
        record.setOutcomeLabel(MatchingOutcomeLabel.POSITIVE_OUTCOME); record.setProjectCompleted(true);
        record.setCompletedOnTime(true); record.setClientRating(5); record.setMilestoneSuccessRate(100.0);
        record.setEvaluatedAt(LocalDateTime.now());
        MatchingEvaluationRecord saved = matchingEvaluationRecordRepository.saveAndFlush(record);

        MatchingEvaluationRecord loaded = matchingEvaluationRecordRepository.findById(saved.getId()).orElseThrow();
        assertEquals(project.getId(), loaded.getProject().getId());
        assertEquals(freelancer.getId(), loaded.getFreelancer().getId());
        assertEquals(proposal.getId(), loaded.getAcceptedProposal().getId());
        assertEquals(MatchingOutcomeLabel.POSITIVE_OUTCOME, loaded.getOutcomeLabel());
        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getEvaluatedAt());
        assertEquals(1, matchingEvaluationRecordRepository.findFiltered(project.getId(), freelancer.getId(),
                MatchingOutcomeLabel.POSITIVE_OUTCOME, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)).size());
    }

    @Test
    void shouldPersistTrustScoreSnapshotForFreelancer() {
        User user = userRepository.save(new User("Trust", "Tester", "trust.snapshot@example.com", "secureHash", Role.FREELANCER, AccountStatus.ACTIVE));
        FreelancerProfile freelancer = freelancerProfileRepository.save(new FreelancerProfile(user, "Engineer", "Bio", "Remote", 3, new BigDecimal("50.00"), "AVAILABLE"));
        TrustHistory snapshot = new TrustHistory();
        snapshot.setFreelancer(freelancer);
        snapshot.setOverallTrustScore(82.5);
        snapshot.setCompletionReliabilityScore(100.0);
        snapshot.setProjectsConsidered(2);
        snapshot.setCalculatedAt(LocalDateTime.now());

        TrustHistory stored = trustHistoryRepository.saveAndFlush(snapshot);

        assertNotNull(stored.getId());
        TrustHistory reloaded = trustHistoryRepository.findById(stored.getId()).orElseThrow();
        assertEquals(freelancer.getId(), reloaded.getFreelancer().getId());
        assertEquals(82.5, reloaded.getOverallTrustScore());
        assertEquals(100.0, reloaded.getCompletionReliabilityScore());
        assertEquals(2, reloaded.getProjectsConsidered());
        assertNotNull(reloaded.getCalculatedAt());
    }

    @Test
    void shouldPersistAndRetrieveUserAndProfiles() {
        // 1. Create and persist client user
        User clientUser = new User("Alice", "Smith", "alice@example.com", "secureHash", Role.CLIENT, AccountStatus.ACTIVE);
        clientUser = userRepository.save(clientUser);
        assertNotNull(clientUser.getId());
        assertNotNull(clientUser.getCreatedAt());

        // 2. Create and persist client profile
        ClientProfile clientProfile = new ClientProfile(clientUser, "Acme Corp", "Tech startup", "Software", "San Francisco", "https://acme.example.com");
        clientProfile = clientProfileRepository.save(clientProfile);
        assertNotNull(clientProfile.getId());

        // 3. Create and persist freelancer user
        User freelancerUser = new User("Bob", "Jones", "bob@example.com", "secureHash", Role.FREELANCER, AccountStatus.ACTIVE);
        freelancerUser = userRepository.save(freelancerUser);
        assertNotNull(freelancerUser.getId());

        // 4. Create and persist freelancer profile
        FreelancerProfile freelancerProfile = new FreelancerProfile(freelancerUser, "Full Stack Engineer", "Java and React expert", "Remote", 5, new BigDecimal("75.00"), "AVAILABLE");
        freelancerProfile = freelancerProfileRepository.save(freelancerProfile);
        assertNotNull(freelancerProfile.getId());

        // 5. Create skill and freelancer skill
        Skill javaSkill = skillRepository.findByNameIgnoreCase("Java").orElseGet(() -> skillRepository.save(new Skill("Java", "Backend")));
        FreelancerSkill freelancerSkill = freelancerSkillRepository.save(new FreelancerSkill(freelancerProfile, javaSkill, SkillProficiency.EXPERT));
        assertNotNull(freelancerSkill.getId());
        assertEquals(SkillProficiency.EXPERT, freelancerSkill.getProficiencyLevel());

        // 6. Create category and project
        Category webCategory = categoryRepository.findByNameIgnoreCase("Web Development").orElseGet(() -> categoryRepository.save(new Category("Web Development", "Frontend and backend web systems")));
        Project project = new Project(
                clientProfile,
                "E-Commerce Microservices",
                "Scalable backend services",
                webCategory,
                new BigDecimal("5000.00"),
                new BigDecimal("10000.00"),
                4,
                LocalDate.now().plusMonths(2),
                ProjectComplexity.HIGH,
                ProjectStatus.OPEN
        );
        project = projectRepository.save(project);
        assertNotNull(project.getId());

        // 7. Attach project skill
        ProjectSkill projectSkill = projectSkillRepository.save(new ProjectSkill(project, javaSkill, SkillImportance.REQUIRED));
        assertNotNull(projectSkill.getId());
        assertEquals(SkillImportance.REQUIRED, projectSkill.getImportance());

        // 8. Submit proposal
        Proposal proposal = new Proposal(
                project,
                freelancerProfile,
                "I have 5 years experience with Java microservices.",
                new BigDecimal("7500.00"),
                30,
                ProposalStatus.SUBMITTED,
                null
        );
        proposal = proposalRepository.save(proposal);
        assertNotNull(proposal.getId());

        // Verify retrieval
        Optional<Proposal> retrieved = proposalRepository.findByProjectIdAndFreelancerId(project.getId(), freelancerProfile.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(new BigDecimal("7500.00"), retrieved.get().getProposedAmount());
    }

    @Test
    void shouldEnforceDuplicateProposalConstraint() {
        User clientUser = userRepository.save(new User("Client2", "User", "client2@example.com", "hash", Role.CLIENT, AccountStatus.ACTIVE));
        ClientProfile clientProfile = clientProfileRepository.save(new ClientProfile(clientUser, "Beta LLC", "Desc", "Finance", "NY", "https://beta.com"));

        User freelancerUser = userRepository.save(new User("Freelancer2", "User", "free2@example.com", "hash", Role.FREELANCER, AccountStatus.ACTIVE));
        FreelancerProfile freelancerProfile = freelancerProfileRepository.save(new FreelancerProfile(freelancerUser, "Dev", "Bio", "Remote", 3, new BigDecimal("50.00"), "AVAILABLE"));

        Project project = projectRepository.save(new Project(
                clientProfile,
                "API Project",
                "Build REST API",
                null,
                new BigDecimal("1000.00"),
                new BigDecimal("2000.00"),
                2,
                LocalDate.now().plusMonths(1),
                ProjectComplexity.MEDIUM,
                ProjectStatus.OPEN
        ));

        // First proposal succeeds
        proposalRepository.saveAndFlush(new Proposal(project, freelancerProfile, "First proposal", new BigDecimal("1500.00"), 10, ProposalStatus.SUBMITTED, null));

        // Second proposal from same freelancer for same project must violate unique constraint
        assertThrows(DataIntegrityViolationException.class, () -> {
            proposalRepository.saveAndFlush(new Proposal(project, freelancerProfile, "Duplicate proposal", new BigDecimal("1400.00"), 8, ProposalStatus.SUBMITTED, null));
        });
    }
}
