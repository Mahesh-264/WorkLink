package com.worklink.dto.matching;

import java.util.List;

/**
 * Immutable DTO representing the result of matching a freelancer against a project.
 * Contains all component scores, matched/missing skills, and a human-readable explanation.
 * Compatibility components remain separate from persisted trust-aware ranking fields.
 */
public record MatchResult(
        int rank,
        Long freelancerId,
        String freelancerName,
        String freelancerHeadline,
        Integer freelancerExperienceYears,
        double compatibilityScore,
        double skillMatchScore,
        double proficiencyScore,
        double experienceScore,
        double portfolioRelevanceScore,
        double budgetCompatibilityScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        String explanation,
        Double trustScore,
        String trustAvailability,
        double finalRankingScore,
        String rankingExplanation,
        Double semanticSimilarity,
        Double semanticRelevanceScore,
        String semanticAvailability,
        String semanticExplanation
) {
    /** Source-compatible constructor for existing callers that only provide compatibility data. */
    public MatchResult(int rank, Long freelancerId, String freelancerName, String freelancerHeadline,
            Integer freelancerExperienceYears, double compatibilityScore, double skillMatchScore,
            double proficiencyScore, double experienceScore, double portfolioRelevanceScore,
            double budgetCompatibilityScore, List<String> matchedSkills, List<String> missingSkills,
            String explanation) {
        this(rank, freelancerId, freelancerName, freelancerHeadline, freelancerExperienceYears,
                compatibilityScore, skillMatchScore, proficiencyScore, experienceScore,
                portfolioRelevanceScore, budgetCompatibilityScore, matchedSkills, missingSkills,
                explanation, null, "UNAVAILABLE", compatibilityScore,
                "Cold start: final ranking uses the compatibility score unchanged.",
                null, null, "UNAVAILABLE", null);
    }

    public MatchResult withTrust(Double score, String availability, double finalScore, String rankingReason) {
        return new MatchResult(rank, freelancerId, freelancerName, freelancerHeadline,
                freelancerExperienceYears, compatibilityScore, skillMatchScore, proficiencyScore,
                experienceScore, portfolioRelevanceScore, budgetCompatibilityScore, matchedSkills,
                missingSkills, explanation, score, availability, finalScore, rankingReason,
                semanticSimilarity, semanticRelevanceScore, semanticAvailability, semanticExplanation);
    }

    public MatchResult withRank(int value) {
        return new MatchResult(value, freelancerId, freelancerName, freelancerHeadline,
                freelancerExperienceYears, compatibilityScore, skillMatchScore,
                proficiencyScore, experienceScore, portfolioRelevanceScore, budgetCompatibilityScore,
                matchedSkills, missingSkills, explanation, trustScore, trustAvailability,
                finalRankingScore, rankingExplanation, semanticSimilarity, semanticRelevanceScore,
                semanticAvailability, semanticExplanation);
    }

    public MatchResult withSemantic(SemanticMatchResult semantic) {
        return new MatchResult(rank, freelancerId, freelancerName, freelancerHeadline,
                freelancerExperienceYears, compatibilityScore, skillMatchScore, proficiencyScore,
                experienceScore, portfolioRelevanceScore, budgetCompatibilityScore, matchedSkills,
                missingSkills, explanation, trustScore, trustAvailability, finalRankingScore,
                rankingExplanation, semantic.semanticSimilarity(), semantic.semanticRelevanceScore(),
                semantic.semanticAvailability(), semantic.explanation());
    }
}
