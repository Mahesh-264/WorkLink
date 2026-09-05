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

## 3. Review-2 Matching Engine Design (Modular Integration)

For Review-2, matching logic is implemented as a modular Spring service (`MatchingService`):
- Accepts: `ProjectRequirementProfile` & list of `FreelancerCapabilityProfile`s.
- Calculates sub-scores:
  1. Skill compatibility score
  2. Experience compatibility score
  3. Portfolio relevance score
  4. Initial trust & reliability score
- Computes aggregate weighted compatibility score.
- Returns ranked recommendation list.

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
