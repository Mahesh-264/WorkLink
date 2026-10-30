package com.worklink.repository;

import com.worklink.entity.MatchingEvaluationRecord;
import com.worklink.entity.enums.MatchingOutcomeLabel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MatchingEvaluationRecordRepositoryImpl implements MatchingEvaluationRecordRepositoryCustom {
    @PersistenceContext private EntityManager entityManager;

    @Override
    public List<MatchingEvaluationRecord> findFiltered(Long projectId, Long freelancerId,
            MatchingOutcomeLabel outcome, LocalDateTime startAt, LocalDateTime endAt) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<MatchingEvaluationRecord> query = builder.createQuery(MatchingEvaluationRecord.class);
        Root<MatchingEvaluationRecord> record = query.from(MatchingEvaluationRecord.class);
        List<Predicate> predicates = new ArrayList<>();
        if (projectId != null) predicates.add(builder.equal(record.get("project").get("id"), projectId));
        if (freelancerId != null) predicates.add(builder.equal(record.get("freelancer").get("id"), freelancerId));
        if (outcome != null) predicates.add(builder.equal(record.get("outcomeLabel"), outcome));
        if (startAt != null) predicates.add(builder.greaterThanOrEqualTo(record.get("evaluatedAt"), startAt));
        if (endAt != null) predicates.add(builder.lessThanOrEqualTo(record.get("evaluatedAt"), endAt));
        query.select(record).where(predicates.toArray(Predicate[]::new))
                .orderBy(builder.desc(record.get("evaluatedAt")), builder.desc(record.get("id")));
        return entityManager.createQuery(query).getResultList();
    }
}
