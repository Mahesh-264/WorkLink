package com.worklink.dto.matching;

import java.util.List;

/** Result of the independent semantic relevance baseline. */
public record SemanticMatchResult(Long projectId, Long freelancerId, Double semanticSimilarity,
        Double semanticRelevanceScore, String semanticAvailability, List<String> projectTermsUsed,
        List<String> freelancerTermsUsed, String explanation) {
    public static SemanticMatchResult unavailable(Long projectId, Long freelancerId, String reason) {
        return new SemanticMatchResult(projectId, freelancerId, null, null, "UNAVAILABLE",
                List.of(), List.of(), reason);
    }
}
