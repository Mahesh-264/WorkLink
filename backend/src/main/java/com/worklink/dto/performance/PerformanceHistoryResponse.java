package com.worklink.dto.performance;
import com.worklink.entity.PerformanceHistory;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record PerformanceHistoryResponse(Long id, Long freelancerId, Long projectId, String projectTitle, Long acceptedProposalId,
 String projectStatus, LocalDate completionDate, LocalDate deadline, Boolean onTime, int milestoneCount, int approvedMilestoneCount,
 Integer clientRating, String clientReview, int freelancerActivityCount, int clientActivityCount, int activityCount,
 LocalDateTime createdAt, LocalDateTime updatedAt) {
 public static PerformanceHistoryResponse from(PerformanceHistory h) { return new PerformanceHistoryResponse(h.getId(), h.getFreelancer().getId(), h.getProject().getId(), h.getProject().getTitle(), h.getAcceptedProposal().getId(), h.getProjectStatus().name(), h.getCompletionDate(), h.getDeadline(), h.getOnTime(), h.getMilestoneCount(), h.getApprovedMilestoneCount(), h.getClientRating(), h.getClientReview(), h.getFreelancerActivityCount(), h.getClientActivityCount(), h.getActivityCount(), h.getCreatedAt(), h.getUpdatedAt()); }
}
