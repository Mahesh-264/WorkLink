package com.worklink.dto.evaluation;

import com.worklink.entity.MatchingEvaluationRecord;
import com.worklink.entity.enums.MatchingOutcomeLabel;

import java.time.LocalDateTime;

public record MatchingEvaluationRecordResponse(Long id, Long projectId, String projectTitle, Long freelancerId,
        Long acceptedProposalId, MatchingOutcomeLabel outcomeLabel, Boolean projectCompleted,
        Boolean completedOnTime, Integer clientRating, Double milestoneSuccessRate,
        LocalDateTime createdAt, LocalDateTime evaluatedAt) {
    public static MatchingEvaluationRecordResponse from(MatchingEvaluationRecord record) {
        return new MatchingEvaluationRecordResponse(record.getId(), record.getProject().getId(),
                record.getProject().getTitle(), record.getFreelancer().getId(),
                record.getAcceptedProposal() == null ? null : record.getAcceptedProposal().getId(),
                record.getOutcomeLabel(), record.getProjectCompleted(), record.getCompletedOnTime(),
                record.getClientRating(), record.getMilestoneSuccessRate(), record.getCreatedAt(), record.getEvaluatedAt());
    }
}
