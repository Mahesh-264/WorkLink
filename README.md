# WorkLink

## Final Academic Title
**WorkLink: A Dynamic Context-Aware Framework for Intelligent Freelancer–Project Matching**

---

## 1. Project Overview
WorkLink is a next-generation full-stack freelance project collaboration platform engineered to bridge the gap between clients, freelancers, and project management ecosystems. While traditional platforms rely predominantly on keyword searches or rigid rule-based filters, WorkLink is designed with an integrated intelligence foundation capable of calculating nuanced, multi-dimensional compatibility between project specifications and freelancer profiles.

---

## 2. Problem Statement
Contemporary freelance marketplaces suffer from critical matching inefficiencies:
- High noise in proposal submissions, overburdening clients with irrelevant bids.
- Inadequate consideration of multi-faceted context such as project complexity, deliverable alignment, historical reliability, and dynamic trust.
- Static rating systems that fail to reflect recent performance velocity or situational suitability.

WorkLink addresses these challenges by developing a holistic, context-aware framework that evaluates both explicit capability markers (skills, experience, portfolio) and dynamic behavioral indicators (trust, past performance, reliability).

---

## 3. Research Direction
The core research pillar of WorkLink is:
> **Dynamic Context-Aware Freelancer–Project Matching**

The matching framework models:
- **Project Requirement Profile:** Required skills, importance weights, required experience level, budget tier, deadline constraints, complexity rating, deliverable expectations, and domain category.
- **Freelancer Capability Profile:** Verified skills, proficiency ratings, cumulative experience, portfolio relevance, availability, and historical deliverable performance.
- **Dynamic Trust & Reliability:** Behavioral signals including completion consistency, deadline adherence, communication efficacy, and recent project momentum.

The matching pipeline keeps a deterministic, explainable Compatibility Score separate from Dynamic Trust Score. Block 7.3 ranks eligible freelancers with `Compatibility × 0.70 + Trust × 0.30` when a persisted trust snapshot is available; cold-start freelancers retain their compatibility score unchanged. Compatibility component formulas and weights remain unchanged.

### Block 7.3 — Trust-Aware Freelancer Ranking
- Client recommendations use the latest persisted `TrustHistory` score and do not recalculate trust during a match request.
- The match response retains every existing compatibility field and adds `trustScore`, `trustAvailability`, `finalRankingScore`, and `rankingExplanation`.
- Missing trust is marked `UNAVAILABLE`; final ranking falls back to compatibility unchanged. Available trust is marked `AVAILABLE`.
- Ranking order is final score descending, compatibility descending, available trust descending, then freelancer ID ascending. The existing limit behavior remains in effect.
- `GET /api/client/projects/{projectId}/matches?limit=10` remains limited to the owning client. `GET /api/freelancer/projects/{projectId}/match` resolves only the authenticated freelancer.
- Default ranking weights are configured as `0.70` compatibility and `0.30` trust under `worklink.matching.ranking`; environment overrides are supported and must sum to `1.0`.
- Backend tests cover persisted snapshot lookup, cold start, weighted re-ranking, deterministic ties, limit behavior, ownership, role restrictions, and self-match privacy. Block 7.4 adds the client and freelancer trust and performance UI. ML/AI, semantic matching, and What-If simulation remain deferred.

### Block 7.4 — Trust & Performance Insights UI
- Client recommendation cards preserve the compatibility explanation and component breakdown while distinguishing Compatibility Score, Dynamic Trust Score, and Final Ranking Score. The final ranking score is identified as the recommendation ordering signal, and its explanation is displayed from the backend `rankingExplanation` field.
- Client cold-start recommendations display “Trust history is not yet available.” They retain compatibility and final ranking scores, and do not display an unavailable trust score as zero.
- The freelancer dashboard loads `GET /api/freelancer/trust-score` and `GET /api/freelancer/performance` independently. The score card shows availability, available component scores, weights and contributions, projects considered, calculation time, server-provided interpretation, and component bases. History records show persisted project outcomes, dates, deadline status, milestone counts, activity counts, and client review fields when present.
- Trust score and performance history have independent loading, retry, error, and empty states. A failure in one request does not prevent the other section or existing dashboard sections from loading.
- **Compatibility Score ≠ Dynamic Trust Score ≠ Final Ranking Score.** Compatibility describes project and freelancer fit. Dynamic Trust summarizes persisted performance signals. Final Ranking is the backend-provided ordering score; for cold-start matches the backend currently falls back to compatibility.
- The matching response currently includes overall trust and ranking fields, but not the trust component breakdown. The client UI only renders that breakdown if component data is present in an eligible matching response; it does not make a separate request for private freelancer history or recalculate components.

### Block 8.3 — Advanced Context-Aware Matching
- The current production baseline remains Compatibility + persisted Dynamic Trust: `Compatibility × 0.70 + Trust × 0.30`. Its compatibility formulas and rank order are unchanged.
- Block 8.2's deterministic TF-IDF/cosine Semantic Relevance result is reused. It is a lexical/statistical baseline, not transformer embeddings or machine learning.
- A separate experimental model, `worklink_context_aware`, combines Compatibility 50%, Semantic Relevance 20%, persisted Dynamic Trust 20%, and a standalone Performance Signal 10%. These initial weights are configurable by `WORKLINK_CONTEXT_COMPATIBILITY_WEIGHT`, `WORKLINK_CONTEXT_SEMANTIC_WEIGHT`, `WORKLINK_CONTEXT_TRUST_WEIGHT`, and `WORKLINK_CONTEXT_PERFORMANCE_WEIGHT`; they must be finite, non-negative, and sum to 1.0. Weight quality has not been established; later evaluation will compare alternatives.
- Unavailable semantic, trust, and performance signals are omitted and the remaining configured weights are renormalized. Missing data is not treated as a zero score. The performance signal is the unweighted mean of available persisted per-project outcome observations (completion/cancellation, known on-time result, milestone approvals, and valid client rating). It does not reuse the Dynamic Trust composite, recency weighting, or activity component, avoiding a duplicate trust score. It still summarizes some of the same underlying outcomes, which is disclosed in the result explanation and should be considered during later evaluation.
- If all available signals happen to have zero configured weight, the available signals receive equal weights so a measurable compatibility-only result remains usable.
- Project-context explanations use persisted required-skill alignment, experience, budget, portfolio, semantic availability, persisted trust availability, persisted performance availability, deadline, and complexity. The `contextScore` is a descriptive arithmetic mean of the existing skill, experience, budget, and portfolio compatibility components; it is reported separately and is not added as a fifth weighted signal.
- `GET /api/client/projects/{projectId}/context-aware-matches?limit=10` requires CLIENT role, ownership, and a published project. `GET /api/freelancer/projects/{projectId}/context-aware-match` requires FREELANCER role and resolves identity from JWT. Neither endpoint writes evaluation records or persists advanced scores. The original `/matches` endpoint remains the production baseline. Precision@K, Recall@K, and NDCG@K are deferred until later evaluation work.

---

## 4. Technology Stack

### Backend
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.x
- **Build Tool:** Maven (via Maven Wrapper)
- **Data Access:** Spring Data JPA & Hibernate
- **Security:** Spring Security & JWT
- **Validation:** Jakarta Bean Validation (Hibernate Validator)
- **Database:** PostgreSQL (v18.4)

### Frontend
- **Framework:** React 19 / Vite
- **Language:** TypeScript
- **Styling:** Tailwind CSS
- **Routing:** React Router v7
- **HTTP Client:** Axios
- **Icons:** Lucide React
- **Forms & Validation:** React Hook Form + Zod
- **Data Visualization:** Recharts

### Future Intelligence Layer (Planned)
- Python 3.12 / FastAPI
- pandas, NumPy, scikit-learn

---

## Authentication and API Access

WorkLink uses stateless JWT access tokens for authenticated API requests. A successful `POST /api/auth/login` response contains a signed Bearer access token plus safe user identity data; password data is never returned. Tokens contain the normalized email as the subject, the user role, issued-at, and expiration claims, and are signed with an HMAC SHA-256 key supplied through `JWT_SECRET`.

- Public endpoints: `GET /api/health`, exposed actuator endpoints, `POST /api/auth/register`, and `POST /api/auth/login`.
- All other `/api/**` endpoints require `Authorization: Bearer <accessToken>`.
- Registration remains limited to `CLIENT` and `FREELANCER`. The authenticated user and authorities are derived from the verified token and current PostgreSQL user record; inactive or deleted users cannot authenticate with an old token.
- Role-based access is enabled through Spring Security authorities (`ROLE_CLIENT`, `ROLE_FREELANCER`) and `@PreAuthorize` for future controller methods.
- The frontend stores only the access token and safe identity data, then its Axios interceptor sends the Bearer header automatically for protected requests. Passwords are never stored.

---

## 5. Current Development Status
- **Current Phase:** **Phase 1 — Project Scaffolding**
- **Completed Milestones:**
  - Workspace inspection & verification
  - Root Git repository initialized
  - Core directory architecture established (`backend/`, `frontend/`, `docs/`)
  - Spring Boot 3 backend foundation with Maven Wrapper and health endpoint (`/api/health`)
  - Vite + React + TypeScript frontend scaffolded with Tailwind CSS, React Router, and Axios
  - Architectural blueprint documented in `docs/architecture.md`
- **Immediate Next Phase:** Phase 2 — Database Configuration & Entity Schema Modeling

---

## 6. Planned Modules
1. **Authentication & Access Control:** JWT-based stateless authentication with strict Role-Based Access Control (`CLIENT`, `FREELANCER`, `ADMIN`).
2. **Profile Management:** Comprehensive Client and Freelancer capability profiles.
3. **Project Management:** End-to-end project lifecycle (Draft, Open, In Review, In Progress, Completed, Closed).
4. **Proposal Submission & Management:** Bidding, cover letters, duration estimates, client shortlisting, and hiring workflows.
5. **Context-Aware Matching Engine:** Multi-parameter deterministic scoring engine for Review-2, progressing toward advanced ML prediction models.
6. **Dynamic Trust & Performance Modeling:** Longitudinal trust scoring reflecting milestone success and client satisfaction.


### Block 8.1 — Matching Evaluation Foundation
- MatchingEvaluationRecord stores one model-neutral observed outcome per project/freelancer pair, with an optional accepted proposal reference and values sourced from persisted PerformanceHistory. A unique project/freelancer constraint prevents duplicate observations.
- Records are captured only when the existing completion workflow creates persisted performance history for the accepted freelancer. Proposal submission or acceptance alone does not create an evaluation label. No all-pairs records, synthetic labels, or historical score snapshots are generated.
- Outcome labels are operational proxies, not perfect ground truth: POSITIVE_OUTCOME means completed with a recorded on-time result; NEGATIVE_OUTCOME means not completed or completed with a recorded late result; INSUFFICIENT_DATA means completed but no deadline outcome is known; NOT_EVALUATED means completion state is unknown. The current capture path is driven by completed-project performance history, so cancellation outcomes are not captured and NOT_EVALUATED is reserved for future observations with unknown completion state.
- Recommendation-time features include project requirements and freelancer capabilities that existed before recommendation, plus only historical performance already known then. Post-outcome data includes completion, deadline result, milestone approvals, client review, activity, and trust snapshots calculated later. Those outcomes must not become features for the recommendation event being evaluated.
- Compatibility, trust, and final ranking scores are not historically persisted by the current matching flow. Block 8.1 does not populate snapshots from current scores; recommendation-time score snapshots remain unavailable until a future flow records them at recommendation time.
- Admin-only research endpoints are GET /api/evaluation/matching and GET /api/evaluation/matching/summary, with optional project, freelancer, outcome, and inclusive date filters. They only read existing evaluation rows. Role.ADMIN already exists, so authentication was not redesigned.
- Future comparison targets can include baseline_skill, baseline_skill_rating, baseline_skill_rating_experience, worklink_deterministic, worklink_trust_aware, future_semantic, and future_ml. This block does not implement or bind records to an algorithm.
- Precision@K, Recall@K, and NDCG@K are deferred until ranked recommendation outputs and corresponding ground-truth observations are sufficient.


### Block 8.2 — Semantic Relevance Baseline
- Match responses include an independent Semantic Relevance Score alongside the existing compatibility and trust-aware ranking fields. The semantic result is calculated after candidate ordering and never changes rank or finalRankingScore.
- Technique: field-weighted token term frequency, smoothed two-document inverse document frequency, TF-IDF vectors, and cosine similarity. semanticSimilarity is bounded to 0–1; semanticRelevanceScore is cosine similarity multiplied by 100 and bounded to 0–100, without rounding.
- Project inputs: title (weight 3), description (1), deliverables (2), and ProjectSkill names (required 3, important 2, preferred 1). Freelancer inputs: headline (3), bio (1), FreelancerSkill names (3), and portfolio title (2), description (1), technologies (2), and role (1.5).
- Text is Unicode NFKC normalized, lowercased with Locale.ROOT, tokenized on punctuation (retaining plus/hash inside technical terms), filtered with a small English stop-word list, and tokens shorter than two characters are removed. Fields that are missing or blank are omitted. No stemming, external model, generated text, or post-project outcome is used.
- If either side has no usable terms, semanticAvailability is UNAVAILABLE and both numeric semantic values are null. Measurable but unrelated text remains AVAILABLE with a zero score.
- This Block 8.2 implementation is a deterministic lexical/statistical semantic baseline. It is not transformer-based semantic understanding or ML ranking.
- Sentence embeddings, transformer embeddings, learned ranking, and ML recommendation remain future research only.
