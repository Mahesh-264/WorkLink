package com.worklink.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Experimental, model-specific weights for Block 8.3. */
@ConfigurationProperties(prefix = "worklink.matching.context-aware")
public class ContextAwareMatchingWeights {
    private double compatibilityWeight = 0.50;
    private double semanticWeight = 0.20;
    private double trustWeight = 0.20;
    private double performanceWeight = 0.10;

    public double getCompatibilityWeight() { return compatibilityWeight; }
    public void setCompatibilityWeight(double value) { compatibilityWeight = value; }
    public double getSemanticWeight() { return semanticWeight; }
    public void setSemanticWeight(double value) { semanticWeight = value; }
    public double getTrustWeight() { return trustWeight; }
    public void setTrustWeight(double value) { trustWeight = value; }
    public double getPerformanceWeight() { return performanceWeight; }
    public void setPerformanceWeight(double value) { performanceWeight = value; }

    @PostConstruct
    void validate() {
        double total = compatibilityWeight + semanticWeight + trustWeight + performanceWeight;
        if (!valid(compatibilityWeight) || !valid(semanticWeight) || !valid(trustWeight)
                || !valid(performanceWeight) || Math.abs(total - 1.0) > 0.000001) {
            throw new IllegalStateException("Context-aware matching weights must be finite, non-negative, and sum to 1.0");
        }
    }

    private boolean valid(double value) { return Double.isFinite(value) && value >= 0.0; }
}
