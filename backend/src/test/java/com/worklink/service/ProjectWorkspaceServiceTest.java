package com.worklink.service;

import com.worklink.dto.ProjectActivityDto;
import com.worklink.entity.Category;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.Project;
import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.ProjectActivityType;
import com.worklink.entity.enums.ProjectComplexity;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.Role;
import com.worklink.repository.ProjectActivityRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectWorkspaceServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectActivityRepository projectActivityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PerformanceHistoryService performanceHistoryService;

    @InjectMocks
    private ProjectWorkspaceService projectWorkspaceService;

    private User clientUser;
    private User freelancerUser;
    private Project project;

    @BeforeEach
    void setUp() {
        clientUser = new User("Client", "One", "client@test.com", "pass", Role.CLIENT, AccountStatus.ACTIVE);
        clientUser.setId(1L);

        freelancerUser = new User("Free", "Lancer", "freelancer@test.com", "pass", Role.FREELANCER, AccountStatus.ACTIVE);
        freelancerUser.setId(2L);

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setUser(clientUser);

        FreelancerProfile freelancerProfile = new FreelancerProfile();
        freelancerProfile.setUser(freelancerUser);

        project = new Project(clientProfile, "Title", "Desc", new Category("Cat", "Desc"), 
                BigDecimal.TEN, BigDecimal.TEN, 1, LocalDate.now(), ProjectComplexity.MEDIUM, ProjectStatus.IN_PROGRESS);
        project.setId(10L);
        project.setAcceptedFreelancer(freelancerProfile);
    }

    @Test
    void addActivity_ByClient_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(clientUser));
        when(projectActivityRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ProjectActivityDto dto = projectWorkspaceService.addActivity(10L, "client@test.com", ProjectActivityType.MESSAGE, "Hello");

        assertThat(dto.content()).isEqualTo("Hello");
        assertThat(dto.type()).isEqualTo(ProjectActivityType.MESSAGE);
        verify(projectActivityRepository).save(any());
    }

    @Test
    void addActivity_ByUnrelatedUser_ThrowsException() {
        User otherUser = new User("Other", "User", "other@test.com", "pass", Role.FREELANCER, AccountStatus.ACTIVE);
        otherUser.setId(3L);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> projectWorkspaceService.addActivity(10L, "other@test.com", ProjectActivityType.MESSAGE, "Hi"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void updateProgress_ByFreelancer_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancerUser));
        when(projectActivityRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ProjectActivityDto dto = projectWorkspaceService.updateProgress(10L, "freelancer@test.com", 50, "Halfway done");

        assertThat(dto.progressPercentage()).isEqualTo(50);
        assertThat(project.getProgressPercentage()).isEqualTo(50);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        verify(projectRepository).save(project);
    }

    @Test
    void updateProgress_To100_CompletesProject() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancerUser));
        when(projectActivityRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        projectWorkspaceService.updateProgress(10L, "freelancer@test.com", 100, "Done");

        assertThat(project.getProgressPercentage()).isEqualTo(100);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verify(performanceHistoryService).createForCompletedProject(project);
    }

    @Test
    void updateProgress_ByClient_ThrowsException() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(clientUser));

        assertThatThrownBy(() -> projectWorkspaceService.updateProgress(10L, "client@test.com", 50, "Update"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only the assigned freelancer can update progress percentage");
    }
}
