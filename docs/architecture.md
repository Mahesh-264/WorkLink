# WorkLink Architecture Documentation

## 1. High-Level Architectural Model (Current: Phase 1)

WorkLink follows a clean, decoupled, layered client-server architecture designed for modularity, strict separation of concerns, and future microservice integration.

```
┌─────────────────────────────────────────────────────────────┐
│                       React Frontend                        │
│         (Vite, TypeScript, Tailwind CSS, React Router)       │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               │ REST APIs (JSON / HTTPS)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                      │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Security Layer                     │  │
│  │     (JWT Filter, SecurityFilterChain, RBAC Auth)      │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              ▼                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   Controller Layer                    │  │
│  │          (REST Endpoints, Request Validation)         │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              ▼                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                     Service Layer                     │  │
│  │  (Business Logic, Transaction Management, Matching)   │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              ▼                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   Repository Layer                    │  │
│  │               (Spring Data JPA / Hibernate)           │  │
│  └───────────────────────────┬───────────────────────────┘  │
└──────────────────────────────┼──────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     PostgreSQL Database                     │
│            (Relational Data, Constraints, Indexes)          │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Backend Layer Breakdown

### 2.1 Controller Layer (`com.worklink.controller`)
- Exposes standardized REST endpoints.
- Validates request payloads via `@Valid` and Jakarta Bean Validation annotations.
- Delegates business processing strictly to the Service layer; maintains no business logic.
- Returns clean DTOs wrapped in consistent HTTP response envelopes.

### 2.2 Service Layer (`com.worklink.service`)
- Coordinates business workflows, validation rules, and transactional boundaries (`@Transactional`).
- Houses the deterministic Context-Aware Matching Engine for Review-2.
- Handles domain object transformations via mappers or DTO converters.

### 2.3 Repository Layer (`com.worklink.repository`)
- Extends Spring Data `JpaRepository`.
- Encapsulates database queries, custom JPQL queries, and database projection operations.

### 2.4 Entity Layer (`com.worklink.entity`)
- Defines JPA relational entities with primary keys, foreign keys, table indexes, and timestamps (`createdAt`, `updatedAt`).
- Entities are never exposed directly to external consumers to prevent cyclic serialization and schema exposure.

### 2.5 Security & Config (`com.worklink.security`, `com.worklink.config`)
- Stateless authentication leverages Spring Security 6, signed JWT access tokens, and no server-side session state.
- `POST /api/auth/login` verifies BCrypt credentials and issues an HMAC SHA-256 JWT. The token subject is the normalized user email and claims include the current role, issued-at, and expiration.
- `JwtAuthenticationFilter` executes before Spring Security's username/password filter. It parses the Bearer token, then loads the user from PostgreSQL and verifies the account remains active before placing an immutable `AuthenticatedUser` principal in the `SecurityContext`.
- Authorities are derived from the database role (`ROLE_CLIENT` or `ROLE_FREELANCER`), not from frontend input. Future endpoints can use `@PreAuthorize("hasRole('CLIENT')")`, `@PreAuthorize("hasRole('FREELANCER')")`, or `CurrentUserProvider` for ownership checks.
- Public API surface: `GET /api/health`, exposed actuator endpoints, and `POST /api/auth/register` / `POST /api/auth/login`. Other `/api/**` routes require `Authorization: Bearer <accessToken>`.
- The React Axios client stores the access token and safe identity data after login, then sends the Authorization header automatically through the existing request interceptor.

---

## 3. Deterministic Matching and Block 7.3 Trust-Aware Ranking

`MatchingScoringService` continues to calculate the existing **Compatibility Score** from five fixed components: Skill Match 40%, Proficiency 20%, Experience 15%, Portfolio Relevance 10%, and Budget Compatibility 15%. These formulas and weights are unchanged.

Block 7.3 adds a separate ranking signal from the latest persisted `TrustHistory` snapshot. It does not recalculate trust during matching or write new trust snapshots.

```text
Final Ranking Score = Compatibility Score × 0.70 + Dynamic Trust Score × 0.30
```

Ranking weights live under `worklink.matching.ranking` in `application.yml` and can be overridden by `WORKLINK_MATCH_COMPATIBILITY_WEIGHT` and `WORKLINK_MATCH_TRUST_WEIGHT`. The configured non-negative weights must sum to 1.0. Final scores are rounded to two decimal places.

When no latest snapshot has a usable `overallTrustScore`, trust is `null` with `trustAvailability=UNAVAILABLE`; the final ranking score falls back to Compatibility Score unchanged. This cold-start behavior avoids penalizing freelancers without history. Available trust is reported as `AVAILABLE`. The client ranking is deterministic: final score descending, compatibility descending, available trust descending, then freelancer ID ascending. The requested limit remains clamped to the existing 1–50 range (default 10).

The match DTO keeps all previous compatibility fields and adds `trustScore`, `trustAvailability`, `finalRankingScore`, and `rankingExplanation`. The existing component `explanation` remains intact. Client recommendations remain restricted to projects owned by the authenticated client; the freelancer endpoint resolves only the authenticated freelancer's own profile. Both endpoints require JWT authentication and their existing role restrictions.

APIs:
- `GET /api/client/projects/{projectId}/matches?limit=10` — ranked recommendations for an owned, published project.
- `GET /api/freelancer/projects/{projectId}/match` — the authenticated freelancer's own match for a published project.

Block 7.3 adds no persistent ranking-score table and does not connect trust to the compatibility component formulas. Dynamic Trust Score remains a separate performance-based signal. ML/AI, semantic matching, What-If simulation, and trust UI are outside this block.

## 4. Block 8.2 Semantic Relevance Baseline

`SemanticRelevanceService` prepares persisted project text (title, description, deliverables, weighted required skills) and freelancer capability text (headline, bio, skills, portfolio title/description/technologies/role). `TfidfCosineSemanticRelevanceEngine` applies deterministic text normalization, tokenization, TF-IDF, and cosine similarity and returns the independent 0–100 Semantic Relevance Score. Empty project or freelancer text is marked unavailable. This is a lexical/statistical baseline, not transformer-based semantic understanding or ML. Its output is already attached to the existing matching result but does not affect baseline ranking.

## 5. Block 8.3 Advanced Context-Aware Matching

### Current baseline

The production recommendation endpoint remains the existing compatibility score combined with the latest persisted Dynamic Trust snapshot at 70%/30%. No compatibility component formula, trust calculation, or existing ranking behavior is changed.

### Experimental context-aware model

The independent `worklink_context_aware` model combines:

```text
Compatibility       50%
Semantic Relevance  20%
Dynamic Trust       20%
Performance         10%
```

The initial weights are experimental, not scientifically optimized. Configure them under `worklink.matching.context-aware` (environment variables: `WORKLINK_CONTEXT_COMPATIBILITY_WEIGHT`, `WORKLINK_CONTEXT_SEMANTIC_WEIGHT`, `WORKLINK_CONTEXT_TRUST_WEIGHT`, `WORKLINK_CONTEXT_PERFORMANCE_WEIGHT`). Weights must be finite, non-negative, and sum to 1.0. Later evaluation will compare alternatives.

If any signal is unavailable, that signal's weight is omitted and the remaining available weights are renormalized. Missing values never become zero. Compatibility is always measurable using the existing deterministic scorer. Semantic relevance reuses the Block 8.2 result; trust uses the latest persisted `TrustHistory` score and does not recalculate trust. The standalone Performance Signal is a simple, unweighted mean of available persisted per-project outcome observations: completed/cancelled status, known on-time status, milestone approval rate, and valid client rating normalized from 1–5 to 0–100. It does not reuse Dynamic Trust's weighted composite, recency component, or activity component. Some underlying outcome facts naturally overlap with trust inputs; the independent aggregation is not claimed to be statistically independent, and later evaluation should account for that.

If all currently available signals have zero configured weight, those available signals receive equal shares as a fallback so the result remains measurable.

The returned `contextScore` is a separate descriptive arithmetic mean of existing skill match, experience, budget compatibility, and portfolio components. It does not contribute an extra weight to the advanced formula. Context explanations also report whether actual project requirements, deadline, complexity, portfolio evidence, semantic text, persisted trust, and historical outcome data are available. No AI-generated explanations or metrics are used.

The research-only endpoints are `GET /api/client/projects/{projectId}/context-aware-matches?limit=10` and `GET /api/freelancer/projects/{projectId}/context-aware-match`. The client route enforces CLIENT role, project ownership, and PUBLISHED status. The freelancer route requires FREELANCER role, a published project, and resolves only the authenticated profile. Results use deterministic ordering: advanced score descending, compatibility descending, available trust descending, freelancer ID ascending. They do not persist scores or create evaluation records. Existing `/matches` remains unchanged. Precision@K, Recall@K, and NDCG@K remain deferred until sufficient ranked predictions and outcome labels are available.

---

## 4. Future Intelligence Layer Architecture (Roadmap)

Following the core stability of the transactional marketplace, the matching engine will seamlessly transition to an asynchronous microservice model:

```
┌─────────────────┐             ┌─────────────────────────┐
│ React Frontend  │             │   PostgreSQL Database   │
└────────┬────────┘             └────────────▲────────────┘
         │                                   │
         ▼                                   ▼
┌─────────────────┐  Internal REST / gRPC  ┌──────────────────────┐
│   Spring Boot   │◄──────────────────────►│    Python/FastAPI    │
│  Core Backend   │                        │  Intelligence Engine │
└─────────────────┘                        └──────────┬───────────┘
                                                      │
                                                      ▼
                                           ┌──────────────────────┐
                                           │ pandas / scikit-learn│
                                           │ ML / Prediction Model│
                                           └──────────────────────┘
```


## 5. Matching Evaluation Foundation (Block 8.1)

MatchingEvaluationRecord is a model-neutral observation linked to a real project, freelancer, and (when available) accepted proposal. The database enforces one record per project/freelancer pair. The existing completion workflow creates a record only after PerformanceHistory has been persisted for the accepted freelancer. Merely submitting or accepting a proposal does not establish relevance and does not create a record.

Outcome labels are explicit, observable proxies rather than perfect ground truth:

- POSITIVE_OUTCOME: the project is recorded completed and the known deadline outcome is on time.
- NEGATIVE_OUTCOME: the project is recorded not completed, or completed with a known late deadline outcome.
- INSUFFICIENT_DATA: the project is completed but no deadline outcome is available (for example, no deadline was recorded).
- NOT_EVALUATED: project completion state is unknown.

The current completed-project capture path derives completion, on-time status, client rating, and milestone approval rate from persisted PerformanceHistory; missing ratings and missing milestone denominators remain null. A score from a current matching request is not a historical score snapshot. Matching score snapshot fields are omitted because the existing recommendation flow does not persist them at recommendation time. Since the application has no cancellation outcome capture workflow, cancelled projects are not currently recorded as negative observations; NOT_EVALUATED is reserved for future observations whose completion state is unknown.

### Leakage boundary

Recommendation-time information comprises project requirements (skills and importance, budget, deadline, complexity, deliverables), freelancer capabilities (skills and proficiency, experience, portfolio, availability), and only historical performance already known before that recommendation. Post-outcome information comprises completion, deadline result, milestone approvals, client review, project activity, final performance, and later Dynamic Trust snapshots. Evaluation outcomes must stay outside the feature window for the recommendation being evaluated; otherwise the evaluation leaks the outcome into its inputs.

Admin-only research access is provided through GET /api/evaluation/matching and GET /api/evaluation/matching/summary. Both support optional project, freelancer, outcome, and inclusive date filters and read only rows that have already been captured. Role.ADMIN exists in the current role model; ordinary client and freelancer roles cannot access these routes.

Future comparison names can include baseline_skill, baseline_skill_rating, baseline_skill_rating_experience, worklink_deterministic, worklink_trust_aware, future_semantic, and future_ml. No algorithms or rankings for these models are implemented in Block 8.1. Precision@K, Recall@K, and NDCG@K are deferred until persisted ranked predictions can be paired with enough observed outcomes.


## 6. Semantic Relevance Baseline (Block 8.2)

The existing deterministic compatibility calculation and trust-aware ordering remain unchanged. A replaceable SemanticRelevanceEngine boundary currently uses a deterministic TF-IDF plus cosine lexical/statistical baseline. MatchingService prepares project text once per recommendation request and only attaches semantic results to already-ordered matches. The semantic signal is not included in the sort comparator or final ranking score.

Project representation uses persisted title (relative weight 3), description (1), deliverables (2), and project skill names (required 3, important 2, preferred 1). Freelancer representation uses persisted headline (3), bio (1), skill names (3), portfolio titles (2), portfolio descriptions (1), technologies (2), and portfolio roles (1.5). All fields are optional in the text builder and missing content is omitted. No completion, review, milestone, activity, trust, or other post-project outcome fields are read.

Preprocessing applies Unicode NFKC normalization, lowercase with Locale.ROOT, punctuation tokenization while preserving plus and hash technical suffixes, a small English stop-word list, and removal of tokens shorter than two characters. There is no stemming. Weighted term frequency is divided by total weighted term frequency within each document. For the two compared documents, smoothed IDF is ln((2 + 1)/(document frequency + 1)) + 1. Each TF-IDF vector is formed by multiplying normalized TF by IDF. Cosine similarity is dot(project, freelancer) / (||project|| × ||freelancer||). The service bounds that value to 0–1, then returns semanticRelevanceScore = semanticSimilarity × 100 without rounding.

If either document has no usable terms, the result is UNAVAILABLE with null similarity and score. If both have usable terms but no overlap, the measurable score is zero. The match response adds semanticSimilarity, semanticRelevanceScore, semanticAvailability, and a deterministic explanation; its existing compatibility fields and finalRankingScore are unaffected.

This Block 8.2 implementation is a deterministic lexical/statistical semantic baseline. It is not transformer-based semantic understanding or ML ranking. Sentence embeddings, transformer embeddings, learned ranking, and ML recommendation remain possible future alternatives behind the engine abstraction and are not implemented here.
