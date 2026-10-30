package com.worklink.service;

import com.worklink.dto.matching.MatchResult;
import com.worklink.dto.matching.ContextAwareMatchResult;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.Project;
import com.worklink.entity.TrustHistory;
import com.worklink.config.ContextAwareMatchingWeights;
import com.worklink.config.MatchingRankingWeights;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.exception.ProfileNotFoundException;
import com.worklink.exception.ResourceNotFoundException;
import com.worklink.repository.ClientProfileRepository;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.TrustHistoryRepository;
import com.worklink.security.CurrentUserProvider;
import com.worklink.service.semantic.SemanticProjectContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Block 4.2 — Matching Service.
 *
 * Orchestrates security checks (project ownership, role) and delegates
 * all scoring to MatchingScoringService so the two concerns stay separate.
 */
@Service
public class MatchingService {

    static final int MAX_LIMIT = 50;
    static final int MIN_LIMIT = 1;

    private final ProjectRepository projectRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final MatchingScoringService scoringService;
    private final TrustHistoryRepository trustHistoryRepository;
    private final MatchingRankingWeights rankingWeights;
    private final SemanticRelevanceService semanticRelevanceService;
    private final ContextAwareMatchingWeights contextWeights;
    private final PerformanceSignalService performanceSignalService;

    public MatchingService(
            ProjectRepository projectRepository,
            ClientProfileRepository clientProfileRepository,
            FreelancerProfileRepository freelancerProfileRepository,
            CurrentUserProvider currentUserProvider,
            MatchingScoringService scoringService,
            TrustHistoryRepository trustHistoryRepository,
            MatchingRankingWeights rankingWeights,
            SemanticRelevanceService semanticRelevanceService,
            ContextAwareMatchingWeights contextWeights,
            PerformanceSignalService performanceSignalService) {
        this.projectRepository         = projectRepository;
        this.clientProfileRepository   = clientProfileRepository;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.currentUserProvider       = currentUserProvider;
        this.scoringService            = scoringService;
        this.trustHistoryRepository    = trustHistoryRepository;
        this.rankingWeights            = rankingWeights;
        this.semanticRelevanceService  = semanticRelevanceService;
        this.contextWeights = contextWeights;
        this.performanceSignalService = performanceSignalService;
    }

    /** Experimental research ranking. The production matches() path remains independent. */
    @Transactional(readOnly = true)
    public List<ContextAwareMatchResult> contextAwareMatches(Long projectId, int limit) {
        ClientProfile client = clientProfileRepository
                .findByUserId(currentUserProvider.requireCurrentUser().id())
                .orElseThrow(ProfileNotFoundException::new);
        Project project = projectRepository.findByIdAndClientId(projectId, client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or access denied"));
        requirePublished(project);

        int safeLimit = Math.min(MAX_LIMIT, Math.max(MIN_LIMIT, limit));
        List<FreelancerProfile> candidates = freelancerProfileRepository.findAll().stream()
                .filter(f -> isEligible(project, f)).toList();
        List<ContextAwareMatchResult> ranked = buildContextAware(project, candidates).stream()
                .sorted(contextAwareComparator()).limit(safeLimit).toList();
        return IntStream.range(0, ranked.size()).mapToObj(i -> withRank(ranked.get(i), i + 1)).toList();
    }

    /** Context-aware result for the authenticated freelancer only. */
    @Transactional(readOnly = true)
    public ContextAwareMatchResult contextAwareMine(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getStatus() == ProjectStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published project not found"));
        FreelancerProfile freelancer = freelancerProfileRepository
                .findByUserId(currentUserProvider.requireCurrentUser().id())
                .orElseThrow(ProfileNotFoundException::new);
        return buildContextAware(project, List.of(freelancer)).getFirst();
    }

    private List<ContextAwareMatchResult> buildContextAware(Project project, List<FreelancerProfile> candidates) {
        if (candidates.isEmpty()) return List.of();
        var semanticProject = semanticRelevanceService.prepareProject(project);
        List<Long> ids = candidates.stream().map(FreelancerProfile::getId).toList();
        Map<Long, TrustHistory> latestTrust = new LinkedHashMap<>();
        trustHistoryRepository.findByFreelancerIdInOrderByCalculatedAtDescIdDesc(ids).forEach(h ->
                latestTrust.putIfAbsent(h.getFreelancer().getId(), h));
        Map<Long, PerformanceSignalService.Signal> performance = performanceSignalService.forFreelancers(ids);
        List<ContextAwareMatchResult> results = new ArrayList<>();
        for (FreelancerProfile freelancer : candidates) {
            MatchResult compatibility = scoringService.score(project, freelancer);
            var semantic = semanticRelevanceService.evaluate(semanticProject, freelancer);
            TrustHistory history = latestTrust.get(freelancer.getId());
            Double trust = history == null ? null : history.getOverallTrustScore();
            if (!isUsableTrustScore(trust)) trust = null;
            var performanceSignal = performance.getOrDefault(freelancer.getId(),
                    new PerformanceSignalService.Signal(null, "UNAVAILABLE", 0));
            Double performanceScore = performanceSignal.score();
            Double semanticScore = "AVAILABLE".equals(semantic.semanticAvailability())
                    ? semantic.semanticRelevanceScore() : null;

            // Compatibility is always measurable, including when there are no required skills.
            double configuredTotal = contextWeights.getCompatibilityWeight()
                    + (semanticScore == null ? 0 : contextWeights.getSemanticWeight())
                    + (trust == null ? 0 : contextWeights.getTrustWeight())
                    + (performanceScore == null ? 0 : contextWeights.getPerformanceWeight());
            int availableCount = 1 + (semanticScore == null ? 0 : 1) + (trust == null ? 0 : 1)
                    + (performanceScore == null ? 0 : 1);
            double normalizationTotal = configuredTotal == 0 ? availableCount : configuredTotal;

            List<ContextAwareMatchResult.SignalContribution> contributions = new ArrayList<>();
            double advanced = 0;
            advanced = addContribution(contributions, "compatibility", compatibility.compatibilityScore(),
                    "AVAILABLE", contextWeights.getCompatibilityWeight(), configuredTotal, normalizationTotal, availableCount, advanced);
            advanced = addContribution(contributions, "semantic", semanticScore, semantic.semanticAvailability(),
                    contextWeights.getSemanticWeight(), configuredTotal, normalizationTotal, availableCount, advanced);
            advanced = addContribution(contributions, "trust", trust, trust == null ? "UNAVAILABLE" : "AVAILABLE",
                    contextWeights.getTrustWeight(), configuredTotal, normalizationTotal, availableCount, advanced);
            advanced = addContribution(contributions, "performance", performanceScore, performanceSignal.availability(),
                    contextWeights.getPerformanceWeight(), configuredTotal, normalizationTotal, availableCount, advanced);
            advanced = round(advanced);
            double contextScore = round((compatibility.skillMatchScore() + compatibility.experienceScore()
                    + compatibility.budgetCompatibilityScore() + compatibility.portfolioRelevanceScore()) / 4.0);
            List<String> contextSignals = contextSignals(project, compatibility, semantic.semanticAvailability(), trust, performanceScore);
            String explanation = String.join(" ", contextSignals);
            results.add(new ContextAwareMatchResult(ContextAwareMatchResult.MODEL, 0, project.getId(),
                    freelancer.getId(), compatibility.freelancerName(), compatibility.compatibilityScore(),
                    semantic.semanticSimilarity(), semantic.semanticRelevanceScore(), semantic.semanticAvailability(),
                    trust, trust == null ? "UNAVAILABLE" : "AVAILABLE", performanceScore,
                    performanceSignal.availability(), contextScore, advanced, explanation,
                    List.copyOf(contributions), List.copyOf(contextSignals)));
        }
        return results;
    }

    private double addContribution(List<ContextAwareMatchResult.SignalContribution> values, String signal,
            Double score, String availability, double configuredWeight, double configuredTotal,
            double normalizationTotal, int availableCount, double sum) {
        Double effective = score == null ? null : configuredTotal == 0
                ? 1.0 / availableCount : configuredWeight / normalizationTotal;
        Double contribution = score == null || effective == null ? null : score * effective;
        values.add(new ContextAwareMatchResult.SignalContribution(signal, score, availability,
                configuredWeight, effective, contribution));
        return contribution == null ? sum : sum + contribution;
    }

    private List<String> contextSignals(Project project, MatchResult match, String semanticAvailability,
            Double trust, Double performance) {
        List<String> details = new ArrayList<>();
        if (match.matchedSkills().isEmpty() && match.missingSkills().isEmpty()) {
            details.add("No required skills were specified for this project.");
        } else if (match.skillMatchScore() >= 80) details.add("Strong alignment with required skills.");
        else if (!match.matchedSkills().isEmpty()) details.add("Partial alignment with required skills.");
        else details.add("No required skill matches were recorded.");
        if (match.experienceScore() >= 100) details.add("The experience requirement is satisfied.");
        else details.add("Experience fit reflects the recorded experience requirement.");
        if (match.budgetCompatibilityScore() >= 80) details.add("Budget compatibility is high.");
        else details.add("Budget compatibility reflects the project budget and freelancer rate or proposal.");
        if (match.portfolioRelevanceScore() > 0) details.add("Relevant portfolio evidence is available.");
        if (project.getDeadline() != null) details.add("Project deadline is included as context; no separate deadline-fit score is available.");
        if (project.getComplexity() != null) details.add("Project complexity is recorded as project context.");
        details.add("Semantic relevance " + ("AVAILABLE".equals(semanticAvailability) ? "is available." : "is unavailable because usable text was insufficient."));
        details.add(trust == null ? "Persisted trust history is unavailable." : "Persisted Dynamic Trust is available.");
        details.add(performance == null ? "Historical performance outcomes are unavailable." : "Historical performance outcomes are available.");
        return details;
    }

    private Comparator<ContextAwareMatchResult> contextAwareComparator() {
        return Comparator.comparingDouble(ContextAwareMatchResult::advancedMatchScore).reversed()
                .thenComparing(Comparator.comparingDouble(ContextAwareMatchResult::compatibilityScore).reversed())
                .thenComparing(ContextAwareMatchResult::trustScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ContextAwareMatchResult::freelancerId);
    }

    private void requirePublished(Project project) {
        if (project.getStatus() != ProjectStatus.PUBLISHED)
            throw new IllegalArgumentException("Recommendations are available only for published projects");
    }

    private ContextAwareMatchResult withRank(ContextAwareMatchResult result, int rank) {
        return new ContextAwareMatchResult(result.model(), rank, result.projectId(), result.freelancerId(),
                result.freelancerName(), result.compatibilityScore(), result.semanticSimilarity(),
                result.semanticRelevanceScore(), result.semanticAvailability(), result.trustScore(),
                result.trustAvailability(), result.performanceScore(), result.performanceAvailability(),
                result.contextScore(), result.advancedMatchScore(), result.explanation(),
                result.signalContributions(), result.contextSignals());
    }

    /**
     * CLIENT endpoint — returns ranked matches for the caller's own project.
     * Ownership is enforced: caller must own the project.
     * Results are sorted by final ranking score, preserving compatibility as its own signal.
     * Limit is clamped to [1, 50].
     */
    @Transactional(readOnly = true)
    public List<MatchResult> matches(Long projectId, int limit) {
        ClientProfile client = clientProfileRepository
                .findByUserId(currentUserProvider.requireCurrentUser().id())
                .orElseThrow(ProfileNotFoundException::new);

        // Ownership check — only the owning client can see their project's matches
        Project project = projectRepository
                .findByIdAndClientId(projectId, client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or access denied"));
        if (project.getStatus() != ProjectStatus.PUBLISHED) {
            throw new IllegalArgumentException("Recommendations are available only for published projects");
        }

        int safeLimit = Math.min(MAX_LIMIT, Math.max(MIN_LIMIT, limit));
        SemanticProjectContext semanticProject = semanticRelevanceService.prepareProject(project);

        List<FreelancerProfile> allFreelancers = freelancerProfileRepository.findAll();

        List<RankedCandidate> ranked = allFreelancers.stream()
                .filter(freelancer -> isEligible(project, freelancer))
                .map(f -> new RankedCandidate(withTrust(scoringService.score(project, f), f), f))
                .sorted(Comparator.comparingDouble((RankedCandidate candidate) -> candidate.match().finalRankingScore()).reversed()
                        .thenComparing(Comparator.comparingDouble((RankedCandidate candidate) -> candidate.match().compatibilityScore()).reversed())
                        .thenComparing(candidate -> candidate.match().trustScore(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(candidate -> candidate.match().freelancerId()))
                .limit(safeLimit)
                .toList();
        return IntStream.range(0, ranked.size())
                .mapToObj(index -> ranked.get(index).match().withRank(index + 1)
                        .withSemantic(semanticRelevanceService.evaluate(semanticProject, ranked.get(index).freelancer())))
                .toList();
    }

    /**
     * FREELANCER endpoint — returns the authenticated freelancer's own compatibility
     * against a specific published project. Identity comes from CurrentUserProvider only.
     */
    @Transactional(readOnly = true)
    public MatchResult mine(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getStatus() == ProjectStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published project not found"));

        FreelancerProfile freelancer = freelancerProfileRepository
                .findByUserId(currentUserProvider.requireCurrentUser().id())
                .orElseThrow(ProfileNotFoundException::new);

        SemanticProjectContext semanticProject = semanticRelevanceService.prepareProject(project);
        return withTrust(scoringService.score(project, freelancer), freelancer)
                .withSemantic(semanticRelevanceService.evaluate(semanticProject, freelancer));
    }

    private MatchResult withTrust(MatchResult compatibility, FreelancerProfile freelancer) {
        TrustHistory snapshot=trustHistoryRepository
                .findTopByFreelancerIdOrderByCalculatedAtDescIdDesc(freelancer.getId())
                .filter(history -> isUsableTrustScore(history.getOverallTrustScore()))
                .orElse(null);
        if(snapshot==null) {
            return compatibility.withTrust(null,"UNAVAILABLE",compatibility.compatibilityScore(),
                    "Cold start: no usable persisted trust score; final ranking uses the compatibility score unchanged.");
        }
        double trust=snapshot.getOverallTrustScore();
        double finalScore=round(rankingWeights.getCompatibilityWeight()*compatibility.compatibilityScore()
                +rankingWeights.getTrustWeight()*trust);
        String reason=String.format(java.util.Locale.ROOT,"%.2f × %.2f + %.2f × %.2f = %.2f",
                compatibility.compatibilityScore(),rankingWeights.getCompatibilityWeight(),trust,
                rankingWeights.getTrustWeight(),finalScore);
        return compatibility.withTrust(trust,"AVAILABLE",finalScore,reason);
    }

    private double round(double score) { return Math.round(score*100.0)/100.0; }

    private boolean isUsableTrustScore(Double score) {
        return score != null && Double.isFinite(score) && score >= 0.0 && score <= 100.0;
    }

    private boolean isEligible(Project project, FreelancerProfile freelancer) {
        return freelancer != null
                && freelancer.getId() != null
                && freelancer.getUser() != null
                && freelancer.getUser().getId() != null
                && !Objects.equals(freelancer.getUser().getId(), project.getClient().getUser().getId())
                && freelancer.getUser().getAccountStatus() == AccountStatus.ACTIVE;
    }

    private record RankedCandidate(MatchResult match, FreelancerProfile freelancer) { }
}
