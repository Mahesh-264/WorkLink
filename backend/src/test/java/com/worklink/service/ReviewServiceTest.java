package com.worklink.service;

import com.worklink.dto.review.ReviewRequest;
import com.worklink.dto.review.ReviewResponse;
import com.worklink.entity.Category;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.Project;
import com.worklink.entity.User;
import com.worklink.entity.enums.ProjectComplexity;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.repository.CategoryRepository;
import com.worklink.repository.ClientProfileRepository;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.ReviewRepository;
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
public class ReviewServiceTest {

    @Autowired private ReviewService reviewService;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ClientProfileRepository clientRepository;
    @Autowired private FreelancerProfileRepository freelancerRepository;
    @Autowired private CategoryRepository categoryRepository;

    private User clientUser;
    private User freelancerUser;
    private User otherUser;
    private Project project;
    private AuthenticatedUser authClient;
    private AuthenticatedUser authFreelancer;
    private AuthenticatedUser authOther;

    @BeforeEach
    void setup() {
        clientUser = userRepository.save(new User("CFirst", "CLast", "client@test.com", "pass", com.worklink.entity.enums.Role.CLIENT, com.worklink.entity.enums.AccountStatus.ACTIVE));
        freelancerUser = userRepository.save(new User("FFirst", "FLast", "freelancer@test.com", "pass", com.worklink.entity.enums.Role.FREELANCER, com.worklink.entity.enums.AccountStatus.ACTIVE));
        otherUser = userRepository.save(new User("OFirst", "OLast", "other@test.com", "pass", com.worklink.entity.enums.Role.CLIENT, com.worklink.entity.enums.AccountStatus.ACTIVE));

        authClient = new AuthenticatedUser(clientUser.getId(), clientUser.getEmail(), com.worklink.entity.enums.Role.CLIENT);
        authFreelancer = new AuthenticatedUser(freelancerUser.getId(), freelancerUser.getEmail(), com.worklink.entity.enums.Role.FREELANCER);
        authOther = new AuthenticatedUser(otherUser.getId(), otherUser.getEmail(), com.worklink.entity.enums.Role.CLIENT);

        ClientProfile cp = clientRepository.save(new ClientProfile(clientUser, "Company", "Tech", "web", "loc", "desc"));
        FreelancerProfile fp = freelancerRepository.save(new FreelancerProfile(freelancerUser, "Dev", "Bio", "url", 5, BigDecimal.TEN, "loc"));

        Category cat = categoryRepository.save(new Category("Dev", "Software"));

        project = new Project(cp, "Title", "Desc", cat, BigDecimal.valueOf(100), BigDecimal.valueOf(1000), 2, LocalDate.now().plusDays(30), ProjectComplexity.MEDIUM, ProjectStatus.COMPLETED);
        project.setAcceptedFreelancer(fp);
        project = projectRepository.save(project);
    }

    @Test
    void testAddReview() {
        // Client reviews freelancer
        ReviewRequest reqClient = new ReviewRequest(5, "Great work!");
        ReviewResponse resClient = reviewService.addReview(project.getId(), reqClient, authClient);
        
        assertThat(resClient.rating()).isEqualTo(5);
        assertThat(resClient.reviewerId()).isEqualTo(clientUser.getId());
        assertThat(resClient.revieweeId()).isEqualTo(freelancerUser.getId());

        // Freelancer reviews client
        ReviewRequest reqFreelancer = new ReviewRequest(4, "Good client.");
        ReviewResponse resFreelancer = reviewService.addReview(project.getId(), reqFreelancer, authFreelancer);
        
        assertThat(resFreelancer.rating()).isEqualTo(4);
        assertThat(resFreelancer.reviewerId()).isEqualTo(freelancerUser.getId());
        assertThat(resFreelancer.revieweeId()).isEqualTo(clientUser.getId());

        // Attempt duplicate review
        assertThrows(IllegalStateException.class, () -> reviewService.addReview(project.getId(), reqClient, authClient));

        // Unauthorized user attempts review
        assertThrows(SecurityException.class, () -> reviewService.addReview(project.getId(), reqClient, authOther));
    }

    @Test
    void testAddReviewToIncompleteProject() {
        project.setStatus(ProjectStatus.IN_PROGRESS);
        projectRepository.save(project);

        assertThrows(IllegalStateException.class, () -> reviewService.addReview(project.getId(), new ReviewRequest(5, "Nice"), authClient));
    }
}
