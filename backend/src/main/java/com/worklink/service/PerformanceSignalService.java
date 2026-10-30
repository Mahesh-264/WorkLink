package com.worklink.service;

import com.worklink.entity.PerformanceHistory;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.repository.PerformanceHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Produces an independent summary of persisted outcome observations. It intentionally does not
 * reuse Dynamic Trust's weighted/recency composite, activity, or TrustHistory components.
 */
@Service
public class PerformanceSignalService {
    private final PerformanceHistoryRepository histories;

    public PerformanceSignalService(PerformanceHistoryRepository histories) { this.histories = histories; }

    public Map<Long, Signal> forFreelancers(Collection<Long> freelancerIds) {
        if (freelancerIds == null || freelancerIds.isEmpty()) return Map.of();
        Map<Long, List<PerformanceHistory>> grouped = histories.findByFreelancerIdIn(freelancerIds).stream()
                .collect(Collectors.groupingBy(h -> h.getFreelancer().getId()));
        Map<Long, Signal> result = new HashMap<>();
        for (Long freelancerId : freelancerIds) result.put(freelancerId, summarize(grouped.getOrDefault(freelancerId, List.of())));
        return result;
    }

    private Signal summarize(List<PerformanceHistory> records) {
        if (records.isEmpty()) return new Signal(null, "UNAVAILABLE", 0);
        double total = 0;
        int observations = 0;
        for (PerformanceHistory row : records) {
            List<Double> outcomeScores = new ArrayList<>(4);
            if (row.getProjectStatus() == ProjectStatus.COMPLETED) outcomeScores.add(100.0);
            else if (row.getProjectStatus() == ProjectStatus.CANCELLED) outcomeScores.add(0.0);
            if (row.getOnTime() != null) outcomeScores.add(row.getOnTime() ? 100.0 : 0.0);
            if (row.getMilestoneCount() > 0) outcomeScores.add(100.0 * row.getApprovedMilestoneCount() / row.getMilestoneCount());
            Integer rating = row.getClientRating();
            if (rating != null && rating >= 1 && rating <= 5) outcomeScores.add((rating - 1) * 25.0);
            if (!outcomeScores.isEmpty()) {
                total += outcomeScores.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
                observations++;
            }
        }
        return observations == 0 ? new Signal(null, "UNAVAILABLE", records.size())
                : new Signal(total / observations, "AVAILABLE", records.size());
    }

    public record Signal(Double score, String availability, int projectsConsidered) { }
}
