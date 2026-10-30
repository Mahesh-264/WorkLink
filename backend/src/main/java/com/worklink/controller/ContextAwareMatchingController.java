package com.worklink.controller;

import com.worklink.dto.matching.ContextAwareMatchResult;
import com.worklink.service.MatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Separate research endpoints for Block 8.3; existing match endpoints remain the baseline. */
@RestController
public class ContextAwareMatchingController {
    private final MatchingService matchingService;

    public ContextAwareMatchingController(MatchingService matchingService) { this.matchingService = matchingService; }

    @GetMapping("/api/client/projects/{projectId}/context-aware-matches")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<ContextAwareMatchResult>> getContextAwareMatches(
            @PathVariable Long projectId, @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(matchingService.contextAwareMatches(projectId, limit));
    }

    @GetMapping("/api/freelancer/projects/{projectId}/context-aware-match")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ContextAwareMatchResult> getMyContextAwareMatch(@PathVariable Long projectId) {
        return ResponseEntity.ok(matchingService.contextAwareMine(projectId));
    }
}
