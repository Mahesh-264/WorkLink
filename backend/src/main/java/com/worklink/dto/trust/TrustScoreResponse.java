package com.worklink.dto.trust;

import java.time.LocalDateTime;
import java.util.List;

public record TrustScoreResponse(Long snapshotId, Double overallTrustScore, String availability, int projectsConsidered,
        LocalDateTime calculatedAt, List<Component> components, String interpretation) {
    public record Component(String name, double weightPercent, Double score, Double contribution, String availability, String basis) {}
}
