package com.worklink.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextAwareMatchingWeightsTest {
    @Test void rejectsNegativeWeight() {
        ContextAwareMatchingWeights weights = new ContextAwareMatchingWeights();
        weights.setSemanticWeight(-0.1);
        assertThatThrownBy(weights::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test void rejectsWeightsThatDoNotSumToOne() {
        ContextAwareMatchingWeights weights = new ContextAwareMatchingWeights();
        weights.setPerformanceWeight(0.2);
        assertThatThrownBy(weights::validate).isInstanceOf(IllegalStateException.class);
    }
}
