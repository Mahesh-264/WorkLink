package com.worklink.repository;

import com.worklink.entity.MatchingEvaluationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchingEvaluationRecordRepository extends JpaRepository<MatchingEvaluationRecord, Long>, MatchingEvaluationRecordRepositoryCustom {
    Optional<MatchingEvaluationRecord> findByProjectIdAndFreelancerId(Long projectId, Long freelancerId);
}
