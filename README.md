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

The resulting output is a deterministic, explainable compatibility score and a ranked recommendation list for project owners.

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
