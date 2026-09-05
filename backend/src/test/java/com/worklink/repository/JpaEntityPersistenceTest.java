package com.worklink.repository;

import com.worklink.entity.Category;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.FreelancerSkill;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                ProposalStatus.SUBMITTED
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
        proposalRepository.saveAndFlush(new Proposal(project, freelancerProfile, "First proposal", new BigDecimal("1500.00"), 10, ProposalStatus.SUBMITTED));

        // Second proposal from same freelancer for same project must violate unique constraint
        assertThrows(DataIntegrityViolationException.class, () -> {
            proposalRepository.saveAndFlush(new Proposal(project, freelancerProfile, "Duplicate proposal", new BigDecimal("1400.00"), 8, ProposalStatus.SUBMITTED));
        });
    }
}
