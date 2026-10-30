package com.worklink.service.semantic;

import java.util.List;

/** Request-scoped project text prepared once and reused across candidate freelancers. */
public record SemanticProjectContext(Long projectId, List<SemanticTextField> fields) {
}
