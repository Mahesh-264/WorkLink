package com.worklink.entity;

import com.worklink.entity.enums.MatchingOutcomeLabel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** A model-neutral record of an observed, accepted freelancer/project outcome. */
@Entity
@Table(name = "matching_evaluation_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_matching_evaluation_project_freelancer", columnNames = {"project_id", "freelancer_id"}),
        indexes = {
                @Index(name = "idx_matching_eval_outcome", columnList = "outcome_label"),
                @Index(name = "idx_matching_eval_evaluated_at", columnList = "evaluated_at"),
                @Index(name = "idx_matching_eval_freelancer", columnList = "freelancer_id")
        })
public class MatchingEvaluationRecord extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private FreelancerProfile freelancer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal acceptedProposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_label", nullable = false, length = 30)
    private MatchingOutcomeLabel outcomeLabel = MatchingOutcomeLabel.NOT_EVALUATED;

    @Column(name = "project_completed")
    private Boolean projectCompleted;

    @Column(name = "completed_on_time")
    private Boolean completedOnTime;

    @Column(name = "client_rating")
    private Integer clientRating;

    @Column(name = "milestone_success_rate")
    private Double milestoneSuccessRate;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public FreelancerProfile getFreelancer() { return freelancer; }
    public void setFreelancer(FreelancerProfile freelancer) { this.freelancer = freelancer; }
    public Proposal getAcceptedProposal() { return acceptedProposal; }
    public void setAcceptedProposal(Proposal acceptedProposal) { this.acceptedProposal = acceptedProposal; }
    public MatchingOutcomeLabel getOutcomeLabel() { return outcomeLabel; }
    public void setOutcomeLabel(MatchingOutcomeLabel outcomeLabel) { this.outcomeLabel = outcomeLabel; }
    public Boolean getProjectCompleted() { return projectCompleted; }
    public void setProjectCompleted(Boolean projectCompleted) { this.projectCompleted = projectCompleted; }
    public Boolean getCompletedOnTime() { return completedOnTime; }
    public void setCompletedOnTime(Boolean completedOnTime) { this.completedOnTime = completedOnTime; }
    public Integer getClientRating() { return clientRating; }
    public void setClientRating(Integer clientRating) { this.clientRating = clientRating; }
    public Double getMilestoneSuccessRate() { return milestoneSuccessRate; }
    public void setMilestoneSuccessRate(Double milestoneSuccessRate) { this.milestoneSuccessRate = milestoneSuccessRate; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
