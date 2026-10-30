package com.worklink.service;

import com.worklink.dto.ProjectActivityDto;
import com.worklink.entity.Project;
import com.worklink.entity.ProjectActivity;
import com.worklink.entity.User;
import com.worklink.entity.enums.ProjectActivityType;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.Role;
import com.worklink.repository.ProjectActivityRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class ProjectWorkspaceService {

    private final ProjectRepository projectRepository;
    private final ProjectActivityRepository projectActivityRepository;
    private final UserRepository userRepository;
    private final PerformanceHistoryService performanceHistoryService;

    @Autowired
    public ProjectWorkspaceService(ProjectRepository projectRepository, ProjectActivityRepository projectActivityRepository, UserRepository userRepository, PerformanceHistoryService performanceHistoryService) {
        this.projectRepository = projectRepository;
        this.projectActivityRepository = projectActivityRepository;
        this.userRepository = userRepository;
        this.performanceHistoryService = performanceHistoryService;
    }

    public ProjectWorkspaceService(ProjectRepository projectRepository, ProjectActivityRepository projectActivityRepository, UserRepository userRepository) {
        this(projectRepository, projectActivityRepository, userRepository, null);
    }


    @Transactional(readOnly = true)
    public List<ProjectActivityDto> getProjectActivities(Long projectId, String userEmail) {
        Project project = getProjectAndValidateAccess(projectId, userEmail);
        return projectActivityRepository.findByProjectIdOrderByCreatedAtAsc(project.getId())
                .stream()
                .map(ProjectActivityDto::fromEntity)
                .toList();
    }

    @Transactional
    public ProjectActivityDto addActivity(Long projectId, String userEmail, ProjectActivityType type, String content) {
        Project project = getProjectAndValidateAccess(projectId, userEmail);
        User sender = userRepository.findByEmail(userEmail).orElseThrow();

        // If it's a progress update, but we are just adding a message/file, the percentage stays the same.
        ProjectActivity activity = new ProjectActivity(project, sender, type, content, project.getProgressPercentage());
        projectActivityRepository.save(activity);
        
        return ProjectActivityDto.fromEntity(activity);
    }

    @Transactional
    public ProjectActivityDto updateProgress(Long projectId, String userEmail, Integer newPercentage, String note) {
        Project project = getProjectAndValidateAccess(projectId, userEmail);
        User sender = userRepository.findByEmail(userEmail).orElseThrow();

        if (sender.getRole() != Role.FREELANCER) {
            throw new IllegalArgumentException("Only the assigned freelancer can update progress percentage.");
        }

        if (newPercentage < 0 || newPercentage > 100) {
            throw new IllegalArgumentException("Progress percentage must be between 0 and 100.");
        }

        project.setProgressPercentage(newPercentage);
        if (newPercentage == 100) {
            project.setStatus(ProjectStatus.COMPLETED);
        }
        projectRepository.save(project);
        if (newPercentage == 100 && performanceHistoryService != null) {
            performanceHistoryService.createForCompletedProject(project);
        }

        ProjectActivity activity = new ProjectActivity(project, sender, ProjectActivityType.PROGRESS_UPDATE, note, newPercentage);
        projectActivityRepository.save(activity);

        return ProjectActivityDto.fromEntity(activity);
    }

    private Project getProjectAndValidateAccess(Long projectId, String userEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found."));

        User user = userRepository.findByEmail(userEmail).orElseThrow();

        boolean isOwner = project.getClient().getUser().getId().equals(user.getId());
        boolean isAssignedFreelancer = project.getAcceptedFreelancer() != null && 
                                       project.getAcceptedFreelancer().getUser().getId().equals(user.getId());

        if (!isOwner && !isAssignedFreelancer) {
            throw new IllegalArgumentException("Access denied. You are not a participant in this project.");
        }
        
        if (project.getStatus() != ProjectStatus.IN_PROGRESS && project.getStatus() != ProjectStatus.COMPLETED) {
            throw new IllegalArgumentException("Workspace is only available for IN_PROGRESS or COMPLETED projects.");
        }

        return project;
    }
}
