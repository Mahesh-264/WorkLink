package com.worklink.dto.matching;

import java.util.List;

/** Independent Block 8.3 result; does not replace MatchResult or its production ranking. */
public record ContextAwareMatchResult(
        String model,
        int rank,
        Long projectId,
        Long freelancerId,
        String freelancerName,
        double compatibilityScore,
        Double semanticSimilarity,
        Double semanticRelevanceScore,
        String semanticAvailability,
        Double trustScore,
        String trustAvailability,
        Double performanceScore,
        String performanceAvailability,
        double contextScore,
        double advancedMatchScore,
        String explanation,
        List<SignalContribution> signalContributions,
        List<String> contextSignals) {

    public static final String MODEL = "worklink_context_aware";

    public record SignalContribution(String signal, Double score, String availability,
            double configuredWeight, Double effectiveWeight, Double weightedContribution) { }
}
