package com.worklink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.annotation.PostConstruct;

/** Central configuration for combining compatibility and persisted trust signals. */
@ConfigurationProperties(prefix = "worklink.matching.ranking")
public class MatchingRankingWeights {
    private double compatibilityWeight = 0.70;
    private double trustWeight = 0.30;
    public double getCompatibilityWeight(){return compatibilityWeight;}
    public void setCompatibilityWeight(double value){compatibilityWeight=value;}
    public double getTrustWeight(){return trustWeight;}
    public void setTrustWeight(double value){trustWeight=value;}
    @PostConstruct
    void validate(){
        if(compatibilityWeight<0||trustWeight<0||Math.abs(compatibilityWeight+trustWeight-1.0)>0.000001)
            throw new IllegalStateException("Matching compatibility and trust ranking weights must be non-negative and sum to 1.0");
    }
}
