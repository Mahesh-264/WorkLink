package com.worklink.service.semantic;

import com.worklink.dto.matching.SemanticMatchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TfidfCosineSemanticRelevanceEngineTest {
    private final TfidfCosineSemanticRelevanceEngine engine = new TfidfCosineSemanticRelevanceEngine();

    @Test void strongLexicalRelevanceScoresHigh() {
        var result = compare("React Node MongoDB e-commerce application",
                "Built React Node MongoDB e-commerce platforms");
        assertThat(result.semanticAvailability()).isEqualTo("AVAILABLE");
        assertThat(result.semanticRelevanceScore()).isGreaterThan(55.0);
        assertThat(result.explanation()).contains("Shared terms:");
    }

    @Test void unrelatedTextScoresLowWithoutArtificialBoost() {
        var result = compare("React e-commerce application", "Accounting and tax consulting");
        assertThat(result.semanticAvailability()).isEqualTo("AVAILABLE");
        assertThat(result.semanticRelevanceScore()).isZero();
    }

    @Test void partialLexicalOverlapProducesIntermediateScore() {
        var result = compare("React Node e-commerce", "React frontend developer with web application experience");
        assertThat(result.semanticAvailability()).isEqualTo("AVAILABLE");
        assertThat(result.semanticRelevanceScore()).isBetween(0.0, 80.0);
    }

    @Test void emptyProjectAndFreelancerTextAreUnavailable() {
        assertThat(engine.evaluate(1L, 2L, List.of(), List.of(field("React developer"))).semanticAvailability()).isEqualTo("UNAVAILABLE");
        assertThat(engine.evaluate(1L, 2L, List.of(field("React project")), List.of()).semanticAvailability()).isEqualTo("UNAVAILABLE");
        assertThat(engine.evaluate(1L, 2L, List.of(field("the and of")), List.of(field("and the of"))).semanticAvailability()).isEqualTo("UNAVAILABLE");
    }

    @Test void calculationIsDeterministicAndScoreIsBounded() {
        List<SemanticTextField> project = List.of(field("React e-commerce platform"));
        List<SemanticTextField> freelancer = List.of(field("React developer building e-commerce apps"));
        SemanticMatchResult first = engine.evaluate(1L, 2L, project, freelancer);
        SemanticMatchResult second = engine.evaluate(1L, 2L, project, freelancer);
        assertThat(second).isEqualTo(first);
        assertThat(first.semanticRelevanceScore()).isBetween(0.0, 100.0);
        assertThat(first.semanticSimilarity()).isBetween(0.0, 1.0);
    }

    private SemanticMatchResult compare(String project, String freelancer) {
        return engine.evaluate(1L, 2L, List.of(field(project)), List.of(field(freelancer)));
    }
    private SemanticTextField field(String text) { return new SemanticTextField(text, 1.0); }
}
