package com.worklink.service;

import com.worklink.dto.milestone.MilestoneRequest;
import com.worklink.dto.milestone.MilestoneResponse;
import com.worklink.dto.milestone.MilestoneReviewRequest;
import com.worklink.dto.milestone.MilestoneSubmissionRequest;
import com.worklink.entity.Milestone;
import com.worklink.entity.Project;
import com.worklink.entity.enums.MilestoneStatus;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.repository.MilestoneRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MilestoneService {

    private final MilestoneRepository milestones;
    private final ProjectRepository projects;
    private final PerformanceHistoryService performanceHistoryService;

    public MilestoneService(MilestoneRepository milestones, ProjectRepository projects, PerformanceHistoryService performanceHistoryService) {
        this.milestones = milestones;
        this.projects = projects;
        this.performanceHistoryService = performanceHistoryService;
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> getProjectMilestones(Long projectId, AuthenticatedUser currentUser) {
        Project p = findAndValidateAccess(projectId, currentUser);
        return milestones.findByProjectIdOrderByDeadlineAsc(projectId).stream().map(this::map).toList();
    }

    @Transactional
    public MilestoneResponse createMilestone(Long projectId, MilestoneRequest request, AuthenticatedUser currentUser) {
        Project p = findAndValidateClient(projectId, currentUser);
        if (p.getStatus() == ProjectStatus.COMPLETED || p.getStatus() == ProjectStatus.CANCELLED || p.getStatus() == ProjectStatus.CLOSED) {
            throw new IllegalStateException("Cannot add milestones to a completed or closed project");
        }
        
        Milestone m = new Milestone(p, request.title(), request.description(), request.amount(), request.deadline());
        return map(milestones.save(m));
    }

    @Transactional
    public MilestoneResponse updateMilestone(Long projectId, Long milestoneId, MilestoneRequest request, AuthenticatedUser currentUser) {
        Project p = findAndValidateClient(projectId, currentUser);
        Milestone m = milestones.findById(milestoneId).orElseThrow(() -> new IllegalArgumentException("Milestone not found"));
        if (!m.getProject().getId().equals(projectId)) throw new IllegalArgumentException("Milestone mismatch");
        
        if (m.getStatus() == MilestoneStatus.APPROVED) {
            throw new IllegalStateException("Cannot edit an approved milestone");
        }

        m.setTitle(request.title());
        m.setDescription(request.description());
        m.setAmount(request.amount());
        m.setDeadline(request.deadline());
        return map(milestones.save(m));
    }

    @Transactional
    public void deleteMilestone(Long projectId, Long milestoneId, AuthenticatedUser currentUser) {
        Project p = findAndValidateClient(projectId, currentUser);
        Milestone m = milestones.findById(milestoneId).orElseThrow(() -> new IllegalArgumentException("Milestone not found"));
        if (!m.getProject().getId().equals(projectId)) throw new IllegalArgumentException("Milestone mismatch");
        
        if (m.getStatus() != MilestoneStatus.PENDING) {
            throw new IllegalStateException("Can only delete PENDING milestones");
        }
        milestones.delete(m);
    }

    @Transactional
    public MilestoneResponse submitMilestone(Long projectId, Long milestoneId, MilestoneSubmissionRequest request, AuthenticatedUser currentUser) {
        Project p = findAndValidateFreelancer(projectId, currentUser);
        Milestone m = milestones.findById(milestoneId).orElseThrow(() -> new IllegalArgumentException("Milestone not found"));
        if (!m.getProject().getId().equals(projectId)) throw new IllegalArgumentException("Milestone mismatch");

        if (m.getStatus() == MilestoneStatus.APPROVED || m.getStatus() == MilestoneStatus.SUBMITTED) {
            throw new IllegalStateException("Invalid milestone status for submission");
        }

        m.setStatus(MilestoneStatus.SUBMITTED);
        m.setSubmissionNote(request.note());
        m.setSubmissionUrl(request.url());
        return map(milestones.save(m));
    }

    @Transactional
    public MilestoneResponse reviewMilestone(Long projectId, Long milestoneId, MilestoneReviewRequest request, AuthenticatedUser currentUser) {
        Project p = findAndValidateClient(projectId, currentUser);
        Milestone m = milestones.findById(milestoneId).orElseThrow(() -> new IllegalArgumentException("Milestone not found"));
        if (!m.getProject().getId().equals(projectId)) throw new IllegalArgumentException("Milestone mismatch");

        if (m.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new IllegalStateException("Can only review SUBMITTED milestones");
        }

        if (request.status() == MilestoneStatus.APPROVED) {
            m.setStatus(MilestoneStatus.APPROVED);
            
            // Check if all milestones are approved to auto-complete project
            boolean allApproved = milestones.findByProjectIdOrderByDeadlineAsc(projectId).stream()
                    .allMatch(ms -> ms.getId().equals(m.getId()) || ms.getStatus() == MilestoneStatus.APPROVED);
                    
            if (allApproved) {
                p.setStatus(ProjectStatus.COMPLETED);
                projects.save(p);
            }
            
        } else if (request.status() == MilestoneStatus.REVISION_REQUESTED) {
            m.setStatus(MilestoneStatus.REVISION_REQUESTED);
        } else {
            throw new IllegalArgumentException("Invalid review status");
        }

        Milestone saved = milestones.save(m);
        if (p.getStatus() == ProjectStatus.COMPLETED) {
            performanceHistoryService.createForCompletedProject(p);
        }
        return map(saved);
    }

    private Project findAndValidateAccess(Long projectId, AuthenticatedUser currentUser) {
        Project p = projects.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Project not found"));
        boolean isClient = p.getClient().getUser().getId().equals(currentUser.id());
        boolean isFreelancer = p.getAcceptedFreelancer() != null && p.getAcceptedFreelancer().getUser().getId().equals(currentUser.id());
        
        if (!isClient && !isFreelancer) {
            throw new SecurityException("Unauthorized access to project milestones");
        }
        return p;
    }

    private Project findAndValidateClient(Long projectId, AuthenticatedUser currentUser) {
        Project p = projects.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (!p.getClient().getUser().getId().equals(currentUser.id())) {
            throw new SecurityException("Only the project client can perform this action");
        }
        return p;
    }
    
    private Project findAndValidateFreelancer(Long projectId, AuthenticatedUser currentUser) {
        Project p = projects.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (p.getAcceptedFreelancer() == null || !p.getAcceptedFreelancer().getUser().getId().equals(currentUser.id())) {
            throw new SecurityException("Only the accepted freelancer can perform this action");
        }
        return p;
    }

    private MilestoneResponse map(Milestone m) {
        return new MilestoneResponse(
                m.getId(),
                m.getProject().getId(),
                m.getTitle(),
                m.getDescription(),
                m.getAmount(),
                m.getDeadline(),
                m.getStatus(),
                m.getSubmissionNote(),
                m.getSubmissionUrl(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
