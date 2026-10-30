package com.worklink.repository;

import com.worklink.entity.MatchingEvaluationRecord;
import com.worklink.entity.enums.MatchingOutcomeLabel;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchingEvaluationRecordRepositoryCustom {
    List<MatchingEvaluationRecord> findFiltered(Long projectId, Long freelancerId,
            MatchingOutcomeLabel outcome, LocalDateTime startAt, LocalDateTime endAt);
}
