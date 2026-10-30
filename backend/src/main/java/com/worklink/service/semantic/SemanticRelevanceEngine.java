package com.worklink.service.semantic;

import com.worklink.dto.matching.SemanticMatchResult;

import java.util.List;

/** Replaceable scoring boundary for future embedding or learned relevance engines. */
public interface SemanticRelevanceEngine {
    SemanticMatchResult evaluate(Long projectId, Long freelancerId,
            List<SemanticTextField> projectFields, List<SemanticTextField> freelancerFields);
}
