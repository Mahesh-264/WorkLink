package com.worklink.controller;

import com.worklink.dto.matching.MatchResult;
import com.worklink.service.MatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Block 4.2 — Matching API endpoints.
 *
 * CLIENT:    GET /api/client/projects/{projectId}/matches?limit=10
 * FREELANCER: GET /api/freelancer/projects/{projectId}/match
 */
@RestController
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /**
     * Returns ranked freelancer matches for the calling client's project.
     * Authenticated CLIENT only. Ownership enforced in service layer.
     *
     * @param projectId the project to match against
     * @param limit     max results to return; clamped to [1, 50]
     */
    @GetMapping("/api/client/projects/{projectId}/matches")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<MatchResult>> getProjectMatches(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(matchingService.matches(projectId, limit));
    }

    /**
     * Returns the authenticated freelancer's own compatibility score for a project.
     * Authenticated FREELANCER only. Freelancer identity resolved server-side.
     *
     * @param projectId the published project to match against
     */
    @GetMapping("/api/freelancer/projects/{projectId}/match")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MatchResult> getMyMatch(@PathVariable Long projectId) {
        return ResponseEntity.ok(matchingService.mine(projectId));
    }
}
