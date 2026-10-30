package com.worklink.service;

import com.worklink.dto.evaluation.MatchingEvaluationRecordResponse;
import com.worklink.dto.evaluation.MatchingEvaluationSummaryResponse;
import com.worklink.entity.MatchingEvaluationRecord;
import com.worklink.entity.PerformanceHistory;
import com.worklink.entity.enums.MatchingOutcomeLabel;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.repository.MatchingEvaluationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MatchingEvaluationService {
    private final MatchingEvaluationRecordRepository records;

    public MatchingEvaluationService(MatchingEvaluationRecordRepository records) {
        this.records = records;
    }

    /** Stores one observed outcome from an existing persisted completion record. */
    @Transactional
    public MatchingEvaluationRecord recordCompletedOutcome(PerformanceHistory history) {
        if (history == null || history.getProject() == null || history.getFreelancer() == null) return null;
        return records.findByProjectIdAndFreelancerId(history.getProject().getId(), history.getFreelancer().getId())
                .orElseGet(() -> {
                    MatchingEvaluationRecord record = new MatchingEvaluationRecord();
                    record.setProject(history.getProject());
                    record.setFreelancer(history.getFreelancer());
                    record.setAcceptedProposal(history.getAcceptedProposal());
                    boolean completed = history.getProjectStatus() == ProjectStatus.COMPLETED;
                    record.setProjectCompleted(completed);
                    record.setCompletedOnTime(history.getOnTime());
                    record.setClientRating(history.getClientRating());
                    record.setMilestoneSuccessRate(history.getMilestoneCount() > 0
                            ? 100.0 * history.getApprovedMilestoneCount() / history.getMilestoneCount() : null);
                    record.setOutcomeLabel(classify(completed, history.getOnTime()));
                    record.setEvaluatedAt(LocalDateTime.now());
                    return records.save(record);
                });
    }

    static MatchingOutcomeLabel classify(Boolean completed, Boolean completedOnTime) {
        if (completed == null) return MatchingOutcomeLabel.NOT_EVALUATED;
        if (!completed) return MatchingOutcomeLabel.NEGATIVE_OUTCOME;
        if (completedOnTime == null) return MatchingOutcomeLabel.INSUFFICIENT_DATA;
        return completedOnTime ? MatchingOutcomeLabel.POSITIVE_OUTCOME : MatchingOutcomeLabel.NEGATIVE_OUTCOME;
    }

    @Transactional(readOnly = true)
    public List<MatchingEvaluationRecordResponse> list(Long projectId, Long freelancerId,
            MatchingOutcomeLabel outcome, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().minusNanos(1);
        return records.findFiltered(projectId, freelancerId, outcome, from, to).stream()
                .map(MatchingEvaluationRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MatchingEvaluationSummaryResponse summary(Long projectId, Long freelancerId,
            MatchingOutcomeLabel outcome, LocalDate fromDate, LocalDate toDate) {
        List<MatchingEvaluationRecord> filtered = filtered(projectId, freelancerId, outcome, fromDate, toDate);
        long completed = filtered.stream().filter(r -> Boolean.TRUE.equals(r.getProjectCompleted())).count();
        long onTime = filtered.stream().filter(r -> Boolean.TRUE.equals(r.getProjectCompleted())
                && Boolean.TRUE.equals(r.getCompletedOnTime())).count();
        List<Integer> ratings = filtered.stream().map(MatchingEvaluationRecord::getClientRating).filter(r -> r != null).toList();
        Double averageRating = ratings.isEmpty() ? null : ratings.stream().mapToInt(Integer::intValue).average().orElseThrow();
        return new MatchingEvaluationSummaryResponse(filtered.size(), count(filtered, MatchingOutcomeLabel.POSITIVE_OUTCOME),
                count(filtered, MatchingOutcomeLabel.NEGATIVE_OUTCOME), count(filtered, MatchingOutcomeLabel.INSUFFICIENT_DATA),
                count(filtered, MatchingOutcomeLabel.NOT_EVALUATED), completed, onTime, averageRating);
    }

    private List<MatchingEvaluationRecord> filtered(Long projectId, Long freelancerId,
            MatchingOutcomeLabel outcome, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().minusNanos(1);
        return records.findFiltered(projectId, freelancerId, outcome, from, to);
    }

    private long count(List<MatchingEvaluationRecord> records, MatchingOutcomeLabel label) {
        return records.stream().filter(r -> r.getOutcomeLabel() == label).count();
    }
}
