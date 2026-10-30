package com.worklink.controller;

import com.worklink.dto.milestone.MilestoneRequest;
import com.worklink.dto.milestone.MilestoneResponse;
import com.worklink.dto.milestone.MilestoneReviewRequest;
import com.worklink.dto.milestone.MilestoneSubmissionRequest;
import com.worklink.security.AuthenticatedUser;
import com.worklink.service.MilestoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'FREELANCER')")
    public List<MilestoneResponse> getProjectMilestones(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return milestoneService.getProjectMilestones(projectId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public MilestoneResponse createMilestone(
            @PathVariable Long projectId,
            @Valid @RequestBody MilestoneRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return milestoneService.createMilestone(projectId, request, user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public MilestoneResponse updateMilestone(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody MilestoneRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return milestoneService.updateMilestone(projectId, id, request, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CLIENT')")
    public void deleteMilestone(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        milestoneService.deleteMilestone(projectId, id, user);
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasRole('FREELANCER')")
    public MilestoneResponse submitMilestone(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody MilestoneSubmissionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return milestoneService.submitMilestone(projectId, id, request, user);
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('CLIENT')")
    public MilestoneResponse reviewMilestone(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody MilestoneReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return milestoneService.reviewMilestone(projectId, id, request, user);
    }
}
