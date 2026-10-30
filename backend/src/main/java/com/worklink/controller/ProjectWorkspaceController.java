package com.worklink.controller;

import com.worklink.dto.ProjectActivityDto;
import com.worklink.entity.enums.ProjectActivityType;
import com.worklink.service.ProjectWorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/workspace")
public class ProjectWorkspaceController {

    private final ProjectWorkspaceService projectWorkspaceService;

    public ProjectWorkspaceController(ProjectWorkspaceService projectWorkspaceService) {
        this.projectWorkspaceService = projectWorkspaceService;
    }

    @GetMapping("/activities")
    public ResponseEntity<List<ProjectActivityDto>> getActivities(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectActivityDto> activities = projectWorkspaceService.getProjectActivities(projectId, userDetails.getUsername());
        return ResponseEntity.ok(activities);
    }

    @PostMapping("/messages")
    public ResponseEntity<ProjectActivityDto> addMessage(
            @PathVariable Long projectId,
            @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectActivityDto activity = projectWorkspaceService.addActivity(projectId, userDetails.getUsername(), ProjectActivityType.MESSAGE, request.content());
        return ResponseEntity.ok(activity);
    }

    @PostMapping("/files")
    public ResponseEntity<ProjectActivityDto> addFile(
            @PathVariable Long projectId,
            @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectActivityDto activity = projectWorkspaceService.addActivity(projectId, userDetails.getUsername(), ProjectActivityType.FILE_UPLOAD, request.content());
        return ResponseEntity.ok(activity);
    }

    @PostMapping("/progress")
    public ResponseEntity<ProjectActivityDto> updateProgress(
            @PathVariable Long projectId,
            @RequestBody ProgressRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectActivityDto activity = projectWorkspaceService.updateProgress(projectId, userDetails.getUsername(), request.percentage(), request.note());
        return ResponseEntity.ok(activity);
    }

    public record ActivityRequest(String content) {}
    public record ProgressRequest(Integer percentage, String note) {}
}
