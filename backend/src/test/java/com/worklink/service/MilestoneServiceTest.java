package com.worklink.service;

import com.worklink.dto.milestone.MilestoneRequest;
import com.worklink.dto.milestone.MilestoneResponse;
import com.worklink.dto.milestone.MilestoneReviewRequest;
import com.worklink.dto.milestone.MilestoneSubmissionRequest;
import com.worklink.entity.Category;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.Milestone;
import com.worklink.entity.Project;
import com.worklink.entity.User;
import com.worklink.entity.enums.MilestoneStatus;
import com.worklink.entity.enums.ProjectComplexity;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.repository.CategoryRepository;
import com.worklink.repository.ClientProfileRepository;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.MilestoneRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.UserRepository;
import com.worklink.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MilestoneServiceTest {

    @Autowired private MilestoneService milestoneService;
    @Autowired private MilestoneRepository milestoneRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ClientProfileRepository clientRepository;
    @Autowired private FreelancerProfileRepository freelancerRepository;
    @Autowired private CategoryRepository categoryRepository;

    private User clientUser;
    private User freelancerUser;
    private Project project;
    private AuthenticatedUser authClient;
    private AuthenticatedUser authFreelancer;

    @BeforeEach
    void setup() {
        clientUser = userRepository.save(new User("CFirst", "CLast", "client@test.com", "pass", com.worklink.entity.enums.Role.CLIENT, com.worklink.entity.enums.AccountStatus.ACTIVE));
        freelancerUser = userRepository.save(new User("FFirst", "FLast", "freelancer@test.com", "pass", com.worklink.entity.enums.Role.FREELANCER, com.worklink.entity.enums.AccountStatus.ACTIVE));

        authClient = new AuthenticatedUser(clientUser.getId(), clientUser.getEmail(), com.worklink.entity.enums.Role.CLIENT);
        authFreelancer = new AuthenticatedUser(freelancerUser.getId(), freelancerUser.getEmail(), com.worklink.entity.enums.Role.FREELANCER);

        ClientProfile cp = clientRepository.save(new ClientProfile(clientUser, "Company", "Tech", "web", "loc", "desc"));
        FreelancerProfile fp = freelancerRepository.save(new FreelancerProfile(freelancerUser, "Dev", "Bio", "url", 5, BigDecimal.TEN, "loc"));

        Category cat = categoryRepository.save(new Category("Dev", "Software"));

        project = new Project(cp, "Title", "Desc", cat, BigDecimal.valueOf(100), BigDecimal.valueOf(1000), 2, LocalDate.now().plusDays(30), ProjectComplexity.MEDIUM, ProjectStatus.IN_PROGRESS);
        project.setAcceptedFreelancer(fp);
        project = projectRepository.save(project);
    }

    @Test
    void testMilestoneLifecycle() {
        // 1. Client creates milestone
        MilestoneRequest req1 = new MilestoneRequest("M1", "Desc1", BigDecimal.valueOf(500), LocalDate.now().plusDays(10));
        MilestoneResponse m1 = milestoneService.createMilestone(project.getId(), req1, authClient);
        
        assertThat(m1.title()).isEqualTo("M1");
        assertThat(m1.status()).isEqualTo(MilestoneStatus.PENDING);

        // Freelancer cannot create milestone
        assertThrows(SecurityException.class, () -> milestoneService.createMilestone(project.getId(), req1, authFreelancer));

        // 2. Freelancer submits
        MilestoneSubmissionRequest subReq = new MilestoneSubmissionRequest("Done", "http://github.com");
        MilestoneResponse submitted = milestoneService.submitMilestone(project.getId(), m1.id(), subReq, authFreelancer);
        
        assertThat(submitted.status()).isEqualTo(MilestoneStatus.SUBMITTED);
        assertThat(submitted.submissionNote()).isEqualTo("Done");

        // 3. Client requests changes
        MilestoneReviewRequest revReq1 = new MilestoneReviewRequest(MilestoneStatus.REVISION_REQUESTED, "Fix this");
        MilestoneResponse revised = milestoneService.reviewMilestone(project.getId(), m1.id(), revReq1, authClient);
        assertThat(revised.status()).isEqualTo(MilestoneStatus.REVISION_REQUESTED);

        // 4. Freelancer submits again
        milestoneService.submitMilestone(project.getId(), m1.id(), new MilestoneSubmissionRequest("Fixed", "url2"), authFreelancer);

        // 5. Client approves -> Project should auto-complete as it is the only milestone
        MilestoneReviewRequest revReq2 = new MilestoneReviewRequest(MilestoneStatus.APPROVED, "Great");
        MilestoneResponse approved = milestoneService.reviewMilestone(project.getId(), m1.id(), revReq2, authClient);
        
        assertThat(approved.status()).isEqualTo(MilestoneStatus.APPROVED);

        Project updatedProject = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(updatedProject.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
    }
}
