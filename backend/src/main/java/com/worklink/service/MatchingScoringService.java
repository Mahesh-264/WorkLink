package com.worklink.service;

import com.worklink.dto.matching.MatchResult;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.PortfolioProject;
import com.worklink.entity.Project;
import com.worklink.entity.ProjectSkill;
import com.worklink.entity.Proposal;
import com.worklink.entity.enums.SkillImportance;
import com.worklink.entity.enums.SkillProficiency;
import com.worklink.repository.FreelancerSkillRepository;
import com.worklink.repository.PortfolioProjectRepository;
import com.worklink.repository.ProjectSkillRepository;
import com.worklink.repository.ProposalRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Block 4.2 — Deterministic Explainable Matching Engine.
 *
 * Scoring weights:
 *   Skill Match          40%
 *   Proficiency          20%
 *   Experience           15%
 *   Portfolio Relevance  10%
 *   Budget Compatibility 15%
 *
 * Each component is independently replaceable for future enhancements
 * (Dynamic Trust, ML/AI, semantic portfolio similarity).
 */
@Service
public class MatchingScoringService {

    // Importance weights
    static final double W_REQUIRED  = 1.0;
    static final double W_IMPORTANT = 0.7;
    static final double W_PREFERRED = 0.4;

    // Proficiency numeric values
    static final double PROF_BEGINNER     = 25.0;
    static final double PROF_INTERMEDIATE = 50.0;
    static final double PROF_ADVANCED     = 75.0;
    static final double PROF_EXPERT       = 100.0;

    // Final weighting
    static final double WEIGHT_SKILL      = 0.40;
    static final double WEIGHT_PROFICIENCY = 0.20;
    static final double WEIGHT_EXPERIENCE  = 0.15;
    static final double WEIGHT_PORTFOLIO   = 0.10;
    static final double WEIGHT_BUDGET      = 0.15;

    private final ProjectSkillRepository projectSkillRepository;
    private final FreelancerSkillRepository freelancerSkillRepository;
    private final PortfolioProjectRepository portfolioProjectRepository;
    private final ProposalRepository proposalRepository;

    public MatchingScoringService(
            ProjectSkillRepository projectSkillRepository,
            FreelancerSkillRepository freelancerSkillRepository,
            PortfolioProjectRepository portfolioProjectRepository,
            ProposalRepository proposalRepository) {
        this.projectSkillRepository = projectSkillRepository;
        this.freelancerSkillRepository = freelancerSkillRepository;
        this.portfolioProjectRepository = portfolioProjectRepository;
        this.proposalRepository = proposalRepository;
    }

    /**
     * Computes a full MatchResult for the given freelancer against the given project.
     */
    public MatchResult score(Project project, FreelancerProfile freelancer) {
        List<ProjectSkill> projectSkills = projectSkillRepository.findByProjectId(project.getId());

        // Map freelancer skills: skillId -> FreelancerSkill
        var freelancerSkillMap = freelancerSkillRepository.findByFreelancerId(freelancer.getId())
                .stream()
                .collect(Collectors.toMap(fs -> fs.getSkill().getId(), Function.identity()));

        // --- Component scores ---
        SkillMatchResult skillResult = calculateSkillMatch(projectSkills, freelancerSkillMap);
        double proficiencyScore = calculateProficiency(projectSkills, freelancerSkillMap);
        double experienceScore  = calculateExperience(
                safeInt(project.getRequiredExperience()),
                safeInt(freelancer.getYearsOfExperience()));
        double portfolioScore   = calculatePortfolioRelevance(project, projectSkills, freelancer);
        double budgetScore      = calculateBudgetCompatibility(project, freelancer);

        // --- Final weighted score ---
        double finalScore = round(
                WEIGHT_SKILL       * skillResult.score()
              + WEIGHT_PROFICIENCY * proficiencyScore
              + WEIGHT_EXPERIENCE  * experienceScore
              + WEIGHT_PORTFOLIO   * portfolioScore
              + WEIGHT_BUDGET      * budgetScore);

        String explanation = generateExplanation(
                skillResult, proficiencyScore, experienceScore, portfolioScore, budgetScore,
                projectSkills.size(), finalScore);

        return new MatchResult(
                0,
                freelancer.getId(),
                freelancer.getUser().getFirstName() + " " + freelancer.getUser().getLastName(),
                freelancer.getHeadline(),
                freelancer.getYearsOfExperience(),
                finalScore,
                round(skillResult.score()),
                round(proficiencyScore),
                round(experienceScore),
                round(portfolioScore),
                round(budgetScore),
                List.copyOf(skillResult.matched()),
                List.copyOf(skillResult.missing()),
                explanation
        );
    }

    // =========================================================================
    // SKILL MATCH  (40%)
    // =========================================================================

    record SkillMatchResult(double score, List<String> matched, List<String> missing) {}

    /**
     * Calculates skill match score 0-100 using importance weights.
     * REQUIRED=1.0, IMPORTANT=0.7, PREFERRED=0.4
     */
    public SkillMatchResult calculateSkillMatch(
            List<ProjectSkill> projectSkills,
            Map<Long, ?> freelancerSkillMap) {

        if (projectSkills.isEmpty()) {
            return new SkillMatchResult(100.0, List.of(), List.of());
        }

        double totalWeight  = 0.0;
        double matchedWeight = 0.0;
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (ProjectSkill ps : projectSkills) {
            double w = importanceWeight(ps.getImportance());
            totalWeight += w;
            if (freelancerSkillMap.containsKey(ps.getSkill().getId())) {
                matchedWeight += w;
                matched.add(ps.getSkill().getName());
            } else {
                missing.add(ps.getSkill().getName());
            }
        }

        double score = totalWeight == 0 ? 0 : 100.0 * matchedWeight / totalWeight;
        return new SkillMatchResult(score, matched, missing);
    }

    // =========================================================================
    // PROFICIENCY  (20%)
    // =========================================================================

    /**
     * Calculates importance-weighted average proficiency across matched skills.
     * Only skills the freelancer actually has contribute to this score.
     */
    public double calculateProficiency(
            List<ProjectSkill> projectSkills,
            Map<Long, com.worklink.entity.FreelancerSkill> freelancerSkillMap) {

        if (projectSkills.isEmpty()) return 0.0;

        double totalWeight  = 0.0;
        double profWeighted = 0.0;

        for (ProjectSkill ps : projectSkills) {
            var fs = freelancerSkillMap.get(ps.getSkill().getId());
            if (fs != null) {
                double w = importanceWeight(ps.getImportance());
                totalWeight  += w;
                profWeighted += w * proficiencyValue(fs.getProficiencyLevel());
            }
        }

        return totalWeight == 0 ? 0.0 : profWeighted / totalWeight;
    }

    // =========================================================================
    // EXPERIENCE  (15%)
    // =========================================================================

    /**
     * Computes experience score 0-100.
     * Meeting/exceeding requirement = 100.
     * Below requirement = proportional reduction.
     * Zero required experience = 100 (no barrier).
     */
    public double calculateExperience(int required, int actual) {
        if (required <= 0) return 100.0;
        if (actual <= 0)   return 0.0;
        // Cap at 100 — no bonus for excess experience beyond requirement
        return Math.min(100.0, 100.0 * actual / required);
    }

    // =========================================================================
    // PORTFOLIO RELEVANCE  (10%)
    // =========================================================================

    /**
     * Deterministic keyword-based relevance check.
     * Compares project skill names against portfolio text (title + description + technologies).
     * Each matched skill name contributes at most once per portfolio project to avoid
     * inflation from keyword repetition.
     */
    public double calculatePortfolioRelevance(
            Project project,
            List<ProjectSkill> projectSkills,
            FreelancerProfile freelancer) {

        if (projectSkills.isEmpty()) return 0.0;

        List<PortfolioProject> portfolio = portfolioProjectRepository.findByFreelancerId(freelancer.getId());
        if (portfolio.isEmpty()) return 0.0;

        Set<String> skillNames = projectSkills.stream()
                .map(ps -> ps.getSkill().getName().toLowerCase())
                .collect(Collectors.toSet());

        // Build a set of distinct skill names that appear in ANY portfolio project
        Set<String> coveredSkills = portfolio.stream()
                .flatMap(pp -> {
                    String text = buildPortfolioText(pp);
                    return skillNames.stream().filter(text::contains);
                })
                .collect(Collectors.toSet());

        // Weight the covered skills by their importance (same as skill match)
        double totalWeight   = 0.0;
        double coveredWeight = 0.0;
        for (ProjectSkill ps : projectSkills) {
            double w = importanceWeight(ps.getImportance());
            totalWeight += w;
            if (coveredSkills.contains(ps.getSkill().getName().toLowerCase())) {
                coveredWeight += w;
            }
        }

        return totalWeight == 0 ? 0.0 : 100.0 * coveredWeight / totalWeight;
    }

    private String buildPortfolioText(PortfolioProject pp) {
        return (Objects.toString(pp.getTitle(), "") + " "
                + Objects.toString(pp.getDescription(), "") + " "
                + Objects.toString(pp.getTechnologies(), "") + " "
                + Objects.toString(pp.getRole(), ""))
                .toLowerCase();
    }

    // =========================================================================
    // BUDGET COMPATIBILITY  (15%)
    // =========================================================================

    /**
     * Computes budget compatibility 0-100.
     * When a proposal exists, uses proposedBudget.
     * Otherwise, uses freelancer hourlyRate (treated as a per-unit proxy).
     * Missing/zero budget → neutral score 50.
     */
    public double calculateBudgetCompatibility(Project project, FreelancerProfile freelancer) {
        Optional<Proposal> proposal = proposalRepository.findByProjectIdAndFreelancerId(
                project.getId(), freelancer.getId());

        BigDecimal rate = proposal
                .map(Proposal::getProposedAmount)
                .orElse(freelancer.getHourlyRate());

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) return 50.0;

        BigDecimal budgetMax = project.getBudgetMax();
        BigDecimal budgetMin = project.getBudgetMin();

        if (budgetMax == null || budgetMax.compareTo(BigDecimal.ZERO) <= 0) return 50.0;

        double rateD = rate.doubleValue();
        double maxD  = budgetMax.doubleValue();
        double minD  = (budgetMin != null && budgetMin.compareTo(BigDecimal.ZERO) > 0)
                       ? budgetMin.doubleValue() : 0.0;

        // Within budget range → full score
        if (rateD >= minD && rateD <= maxD) return 100.0;

        // Above budget max → penalise proportionally
        if (rateD > maxD) {
            double excess = rateD - maxD;
            double penalty = 100.0 * excess / maxD;
            return Math.max(0.0, 100.0 - penalty);
        }

        // Below minimum → slight reduction (client still gets value)
        double deficit = minD - rateD;
        double penalty = 50.0 * deficit / minD;
        return Math.max(50.0, 100.0 - penalty);
    }

    // =========================================================================
    // EXPLANATION
    // =========================================================================

    /**
     * Generates a human-readable explanation based on actual computed scores.
     * Wording corresponds accurately to the calculated result.
     */
    public String generateExplanation(
            SkillMatchResult skillResult,
            double proficiencyScore,
            double experienceScore,
            double portfolioScore,
            double budgetScore,
            int totalProjectSkills,
            double finalScore) {

        int matched = skillResult.matched().size();
        StringBuilder sb = new StringBuilder();

        // Overall quality label
        if (finalScore >= 75) {
            sb.append("Strong match");
        } else if (finalScore >= 50) {
            sb.append("Good match");
        } else if (finalScore >= 25) {
            sb.append("Partial match");
        } else {
            sb.append("Limited match");
        }

        // Skill summary
        sb.append(" with ").append(matched).append("/").append(totalProjectSkills)
          .append(" required skill").append(totalProjectSkills != 1 ? "s" : "");

        if (!skillResult.matched().isEmpty()) {
            String topSkills = skillResult.matched().stream().limit(3).collect(Collectors.joining(", "));
            sb.append(" (").append(topSkills);
            if (skillResult.matched().size() > 3) sb.append(", …");
            sb.append(")");
        }
        sb.append(".");

        // Proficiency qualifier
        if (proficiencyScore >= 80) {
            sb.append(" Expert-level proficiency on matched skills.");
        } else if (proficiencyScore >= 60) {
            sb.append(" Advanced proficiency on matched skills.");
        } else if (proficiencyScore >= 40) {
            sb.append(" Intermediate proficiency on matched skills.");
        } else if (proficiencyScore > 0) {
            sb.append(" Beginner-level proficiency on matched skills.");
        }

        // Experience
        if (experienceScore >= 100) {
            sb.append(" Meets or exceeds experience requirement.");
        } else if (experienceScore >= 60) {
            sb.append(" Partially meets experience requirement.");
        } else if (experienceScore > 0) {
            sb.append(" Below required experience level.");
        }

        // Portfolio
        if (portfolioScore >= 70) {
            sb.append(" Highly relevant portfolio work.");
        } else if (portfolioScore >= 35) {
            sb.append(" Partially relevant portfolio.");
        } else if (portfolioScore > 0) {
            sb.append(" Limited portfolio relevance.");
        } else {
            sb.append(" No relevant portfolio entries found.");
        }

        // Missing skills
        if (!skillResult.missing().isEmpty()) {
            String missing = skillResult.missing().stream().limit(3).collect(Collectors.joining(", "));
            sb.append(" Missing: ").append(missing);
            if (skillResult.missing().size() > 3) sb.append(", …");
            sb.append(".");
        }

        return sb.toString();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    double importanceWeight(SkillImportance importance) {
        return switch (importance) {
            case REQUIRED  -> W_REQUIRED;
            case IMPORTANT -> W_IMPORTANT;
            case PREFERRED -> W_PREFERRED;
        };
    }

    double proficiencyValue(SkillProficiency proficiency) {
        return switch (proficiency) {
            case BEGINNER     -> PROF_BEGINNER;
            case INTERMEDIATE -> PROF_INTERMEDIATE;
            case ADVANCED     -> PROF_ADVANCED;
            case EXPERT       -> PROF_EXPERT;
        };
    }

    int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
