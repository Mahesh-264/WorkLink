package com.worklink.dto.evaluation;

public record MatchingEvaluationSummaryResponse(long totalRecords, long positiveOutcomes,
        long negativeOutcomes, long insufficientDataRecords, long notEvaluatedRecords,
        long completedProjects, long completedOnTimeProjects, Double averageClientRating) {
}
