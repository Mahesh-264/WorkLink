package com.worklink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trust_history", indexes = @Index(name = "idx_trust_history_freelancer_calculated", columnList = "freelancer_id, calculated_at"))
public class TrustHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "freelancer_id", nullable = false) private FreelancerProfile freelancer;
    @Column(name = "overall_trust_score") private Double overallTrustScore;
    @Column(name = "completion_reliability_score") private Double completionReliabilityScore;
    @Column(name = "deadline_adherence_score") private Double deadlineAdherenceScore;
    @Column(name = "client_satisfaction_score") private Double clientSatisfactionScore;
    @Column(name = "milestone_performance_score") private Double milestonePerformanceScore;
    @Column(name = "communication_activity_score") private Double communicationActivityScore;
    @Column(name = "recent_performance_score") private Double recentPerformanceScore;
    @Column(name = "projects_considered", nullable = false) private int projectsConsidered;
    @Column(name = "calculated_at", nullable = false) private LocalDateTime calculatedAt;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public FreelancerProfile getFreelancer(){return freelancer;} public void setFreelancer(FreelancerProfile v){freelancer=v;}
    public Double getOverallTrustScore(){return overallTrustScore;} public void setOverallTrustScore(Double v){overallTrustScore=v;}
    public Double getCompletionReliabilityScore(){return completionReliabilityScore;} public void setCompletionReliabilityScore(Double v){completionReliabilityScore=v;}
    public Double getDeadlineAdherenceScore(){return deadlineAdherenceScore;} public void setDeadlineAdherenceScore(Double v){deadlineAdherenceScore=v;}
    public Double getClientSatisfactionScore(){return clientSatisfactionScore;} public void setClientSatisfactionScore(Double v){clientSatisfactionScore=v;}
    public Double getMilestonePerformanceScore(){return milestonePerformanceScore;} public void setMilestonePerformanceScore(Double v){milestonePerformanceScore=v;}
    public Double getCommunicationActivityScore(){return communicationActivityScore;} public void setCommunicationActivityScore(Double v){communicationActivityScore=v;}
    public Double getRecentPerformanceScore(){return recentPerformanceScore;} public void setRecentPerformanceScore(Double v){recentPerformanceScore=v;}
    public int getProjectsConsidered(){return projectsConsidered;} public void setProjectsConsidered(int v){projectsConsidered=v;}
    public LocalDateTime getCalculatedAt(){return calculatedAt;} public void setCalculatedAt(LocalDateTime v){calculatedAt=v;}
}
