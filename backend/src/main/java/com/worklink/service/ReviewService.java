package com.worklink.service;

import com.worklink.dto.review.ReviewRequest;
import com.worklink.dto.review.ReviewResponse;
import com.worklink.entity.Project;
import com.worklink.entity.Review;
import com.worklink.entity.User;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.ReviewRepository;
import com.worklink.repository.UserRepository;
import com.worklink.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final ProjectRepository projects;
    private final UserRepository users;

    public ReviewService(ReviewRepository reviews, ProjectRepository projects, UserRepository users) {
        this.reviews = reviews;
        this.projects = projects;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProjectReviews(Long projectId) {
        return reviews.findByProjectId(projectId).stream().map(this::map).toList();
    }

    @Transactional
    public ReviewResponse addReview(Long projectId, ReviewRequest request, AuthenticatedUser currentUser) {
        Project p = projects.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        if (p.getStatus() != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("Reviews can only be added to COMPLETED projects");
        }

        User reviewer = users.findById(currentUser.id()).orElseThrow();
        User reviewee;

        boolean isClient = p.getClient().getUser().getId().equals(currentUser.id());
        boolean isFreelancer = p.getAcceptedFreelancer() != null && p.getAcceptedFreelancer().getUser().getId().equals(currentUser.id());

        if (isClient) {
            reviewee = p.getAcceptedFreelancer().getUser();
        } else if (isFreelancer) {
            reviewee = p.getClient().getUser();
        } else {
            throw new SecurityException("Unauthorized to review this project");
        }
        
        Optional<Review> existing = reviews.findByProjectId(projectId).stream()
            .filter(r -> r.getReviewer().getId().equals(reviewer.getId()))
            .findFirst();
            
        if (existing.isPresent()) {
            throw new IllegalStateException("You have already reviewed this project");
        }

        Review review = new Review(p, reviewer, reviewee, request.rating(), request.feedback());
        return map(reviews.save(review));
    }

    private ReviewResponse map(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getProject().getId(),
                r.getReviewer().getId(),
                r.getReviewer().getFirstName() + " " + r.getReviewer().getLastName(),
                r.getReviewee().getId(),
                r.getReviewee().getFirstName() + " " + r.getReviewee().getLastName(),
                r.getRating(),
                r.getFeedback(),
                r.getCreatedAt()
        );
    }
}
