package com.worklink.controller;

import com.worklink.dto.review.ReviewRequest;
import com.worklink.dto.review.ReviewResponse;
import com.worklink.security.AuthenticatedUser;
import com.worklink.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ReviewResponse> getProjectReviews(@PathVariable Long projectId) {
        return reviewService.getProjectReviews(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENT', 'FREELANCER')")
    public ReviewResponse addReview(
            @PathVariable Long projectId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return reviewService.addReview(projectId, request, user);
    }
}
