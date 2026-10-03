# Spring AWS Portfolio

**English** | [日本語](./README.ja.md)

A production-oriented full-stack cloud portfolio project built around a **non-clinical personality assessment product**.

The application is designed to let users complete versioned personality assessments, resume unfinished attempts, receive deterministic results, optionally clarify ambiguous dimensions, retain assessment history, and eventually share selected completed results with groups under explicit consent.

The project is intentionally developed as an end-to-end engineering exercise rather than a feature-only demo: **domain modeling → API design → persistence → security → testing → frontend → containerization → AWS → CI/CD → Infrastructure as Code**.

> **Current backend:** Assessment **Step 7 complete — deterministic Clarification + Tie-break workflow**<br>
> **Current frontend:** **F1-F6 complete for Authentication + the deterministic Assessment flow through Result/History**<br>
> **Next active focus:** **AWS deployment / containerization / Terraform / GitHub Actions CI/CD**. Provider-backed LLM interaction, Group/Sharing, and cross-domain historical deletion remain deferred.

---

## Overview

The final MVP combines two main product areas:

1. **Personality Assessment**
   - register and authenticate;
   - start or resume a versioned assessment attempt;
   - autosave questionnaire progress;
   - submit through an immutable business boundary;
   - calculate deterministic scores and ambiguity;
   - optionally resolve ambiguous dimensions through AI-assisted clarification or user tie-break;
   - retain and manage historical completed assessments.

2. **Group & Explicit Sharing**
   - create or join groups;
   - manage membership and admin responsibilities;
   - explicitly select one completed assessment to share with a group;
   - keep questionnaire answers, private clarification context, complete history, and account data private by default.

The product itself is deliberately non-clinical. AI is treated as a constrained supporting component rather than the source of truth for assessment scoring or lifecycle decisions.

---

## Architecture

The system is designed as a **modular monolith** for the MVP. Business modules remain isolated through package boundaries, narrow public contracts, architecture tests, and explicit Application/Domain/Infrastructure separation without introducing microservices prematurely.

```mermaid
flowchart TD
    Browser["React + TypeScript<br/>Frontend - Auth + Assessment F1-F6 implemented"]

    subgraph Backend[Java 21 + Spring Boot Modular Monolith]
        Identity[Identity]
        Assessment[Assessment]
        Group[Group - planned]
        AI[AI Integration - planned]
    end

    DB[(PostgreSQL)]
    Provider["External LLM Provider<br/>planned"]
    AWS["AWS Deployment<br/>next active focus"]

    Browser -->|REST / JSON| Backend
    Identity --> DB
    Assessment --> DB
    Group --> DB
    Assessment --> AI
    AI --> Provider
    Backend -. deploy .-> AWS
```

### Current backend flow

```text
Register / Login / Session / CSRF
                ↓
        Assessment Catalog
                ↓
      Start / Resume / Restart
                ↓
   Session-bound Questionnaire
                ↓
             Autosave
                ↓
              Submit
                ↓
 Deterministic Scoring + Ambiguity
                ↓
   ┌────────────┴─────────────┐
   │                          │
No ambiguity              Ambiguous
   │                          │
   ↓                          ↓
Complete           Clarification lifecycle boundary
                              ↓
                     Skip current / remaining
                              ↓
                     Exact tie if unresolved?
                              ↓
                       User Tie-break
                              ↓
                           Complete
                ↓
History → Historical Detail

Provider-backed AI conversation remains Backend Step 8.
```

The React frontend currently consumes this deterministic flow end-to-end through the browser, including Session/CSRF authentication, questionnaire autosave/submission, deterministic Skip/Tie-break interaction, Result provenance and completed History navigation.

For the detailed architecture baseline, see [`docs/architecture.md`](docs/architecture.md).

---

## Engineering Highlights

This project is intended to demonstrate more than basic CRUD implementation. The main engineering decisions currently represented in the codebase are:

### 1. Versioned assessment execution

Every `AssessmentSession` binds to one exact `AssessmentDefinitionVersion`. A running or historical attempt never silently upgrades when the assessment definition changes.

This makes deterministic assessment behavior explainable and keeps historical results tied to the specification that actually produced them.

See [`ADR-0002`](docs/adr/ADR-0002-bind-session-to-exact-definition-version.md).

### 2. Deterministic core with bounded AI assistance

Questionnaire scoring and ambiguity detection are deterministic. AI clarification is only eligible for dimensions that the deterministic calculation has already classified as ambiguous.

The LLM therefore does **not** own scoring, assessment lifecycle transitions, or final business invariants.

See [`ADR-0003`](docs/adr/ADR-0003-deterministic-scoring-with-optional-ai.md) and [`ADR-0004`](docs/adr/ADR-0004-dimension-scoped-clarification.md).

### 3. Separate initial and final results

The domain distinguishes deterministic evidence from the final assessment outcome. `InitialAssessmentResult` is immutable and cannot be rewritten by later clarification.

This preserves traceability between the original questionnaire result and any later clarification/tie-break decision.

See [`ADR-0005`](docs/adr/ADR-0005-separate-initial-and-final-results.md).

### 4. Concurrency invariants enforced by PostgreSQL

Critical workflow rules are not delegated to frontend checks or in-memory state. The backend combines local transactions, targeted row locking, constraints, and PostgreSQL partial unique indexes for invariants such as one active Assessment Session per user and assessment definition.

See [`ADR-0013`](docs/adr/ADR-0013-targeted-locking-and-partial-unique-indexes.md).

### 5. Domain model separated from JPA persistence model

Domain objects are kept separate from JPA entities so persistence mapping does not become the domain model by default. Repository adapters translate between the two boundaries.

See [`ADR-0010`](docs/adr/ADR-0010-separate-domain-and-jpa-models.md).

### 6. Real PostgreSQL integration testing

Persistence and concurrency integration tests run against real PostgreSQL through **Testcontainers** rather than H2 because the implementation depends on PostgreSQL-specific behavior such as JSONB, partial indexes, constraints, and row locking.

### 7. Server-side authentication with explicit CSRF protection

Authentication uses Spring Security with server-side sessions persisted by Spring Session JDBC. CSRF remains enabled for state-changing browser requests instead of being disabled for convenience.

See [`ADR-0014`](docs/adr/ADR-0014-server-side-session-authentication.md).

### 8. Clarification as an independently persisted workflow

Implementation feedback led to `DimensionClarification` becoming a separate Assessment Aggregate instead of remaining an in-memory child of `AssessmentSession`. Short Session locks, local transaction boundaries, and database uniqueness constraints coordinate the two lifecycles without holding a database transaction across an external LLM call.

See [`ADR-0016`](docs/adr/ADR-0016-dimension-clarification-separate-aggregate.md).

### 9. Backend-authoritative React workflow

The frontend keeps stable resource URLs and renders Questionnaire, Clarification, Tie-break and Result as views of the authoritative `AssessmentSessionResponse`. TanStack Query owns server state, unsaved questionnaire edits remain local, and mutations update authoritative Session cache rather than manually advancing a duplicate client state machine.

Real-browser integration also exposed issues that mocked frontend tests alone could not prove, including a Restart response-shape mismatch, React StrictMode autosave lifecycle behavior, and an exact-tie backend response-mapping failure after successful finalization.

See [`docs/frontend/05-implementation-checkpoint-f1-f6.md`](docs/frontend/05-implementation-checkpoint-f1-f6.md).

---

## Tech Stack

| Area | Technology | Current status |
|---|---|---|
| Backend | Java 21, Spring Boot 4.1.1 | ✅ Implemented |
| Security | Spring Security, Spring Session JDBC, CSRF | ✅ Implemented |
| Database | PostgreSQL 18 | ✅ Implemented |
| Persistence | JPA / Hibernate, Flyway | ✅ Implemented |
| API | REST, OpenAPI 3.1 | ✅ Implemented through Assessment Step 7 |
| Testing | JUnit 5, Spring MVC Test, ArchUnit, Testcontainers, Vitest, RTL, MSW | ✅ Implemented for current Backend + Frontend scope |
| Local environment | Docker Compose | ✅ PostgreSQL environment implemented |
| Frontend | React + TypeScript, Vite, React Router, TanStack Query | ✅ Auth + deterministic Assessment flow through F6 |
| AI integration | External LLM behind an adapter boundary | ⏸ Deferred until after Cloud/CI-CD |
| Containerization | Docker application image | 🚧 Next active focus |
| Cloud | AWS | 🚧 Next active focus |
| CI/CD | GitHub Actions | 🚧 Next active focus |
| Infrastructure as Code | Terraform | 🚧 Next active focus |

---

## Current Project Status

| Workstream | Status |
|---|---|
| Product requirements & architecture baseline | ✅ Complete |
| Sixteen Personality specification | ✅ Complete |
| Group domain design | ✅ Complete |
| Backend module / persistence / API design | ✅ Complete |
| Identity & Authentication | ✅ Complete |
| Assessment catalog + Start / Resume | ✅ Complete |
| Questionnaire autosave + Submit | ✅ Complete |
| Deterministic scoring + ambiguity | ✅ Complete |
| Restart / Start New | ✅ Complete |
| Assessment History + Historical Detail | ✅ Complete |
| Clarification + Tie-break mutation | ✅ Complete |
| External AI adapter / runtime context | ⏸ Deferred until after Cloud/CI-CD |
| Historical assessment deletion | ⏸ Deferred until Group sharing backend exists |
| Group / Membership / Sharing implementation | ⏳ Planned after current Cloud/CI-CD work |
| React frontend | ✅ F1-F6 complete for current executable Auth + deterministic Assessment scope |
| Docker application image | 🚧 Next active focus |
| AWS deployment | 🚧 Next active focus |
| GitHub Actions CI/CD | 🚧 Next active focus |
| Terraform infrastructure | 🚧 Next active focus |

The detailed roadmap is maintained in [`docs/roadmap.md`](docs/roadmap.md).

---

## Implemented Backend Capabilities

### Identity & Security

- user registration;
- login and logout;
- authenticated `/me` identity context;
- Spring Security server-side Session authentication;
- Spring Session JDBC persistence;
- CSRF token endpoint and validation;
- Argon2id password hashing behind an Infrastructure adapter.

### Assessment

- available Assessment catalog;
- exact DefinitionVersion binding;
- Start / Resume;
- explicit Restart / Start New;
- Session-bound questionnaire reads;
- questionnaire autosave;
- immutable submission boundary;
- deterministic scoring;
- ambiguity detection;
- immediate finalization when clarification is unnecessary;
- persisted Clarification lifecycle and stale external-result protection;
- Skip Current / Skip Remaining deterministic clarification mutations;
- explicit exact-tie user decision and deterministic post-clarification finalization;
- completed Assessment History;
- historical Assessment detail;
- PostgreSQL-backed read projections;
- retry / concurrency / recovery coverage for implemented critical paths.

Not yet implemented in the executable backend are provider-backed LLM interaction, historical deletion orchestration and Group/Sharing. Historical deletion is intentionally blocked on the Group sharing boundary because active shares must be ended before hard deletion.

### Frontend

- React + TypeScript + Vite application shell;
- Register / Login / current-user Session restoration / protected routes / Logout;
- in-memory CSRF management with stale-token refresh-and-retry;
- Assessment Catalog / Detail / Start / Resume / Start New;
- canonical `/assessment-sessions/:sessionId` workflow route;
- Session-bound questionnaire rendering and persisted-answer restoration;
- debounced serialized full-snapshot autosave with save/error/retry state;
- final Submit coordinated with in-flight autosave and already-submitted recovery;
- deterministic Clarification read model, Skip Current and Skip Remaining;
- exact-tie Tie-break interaction and deterministic finalization;
- Result read model with per-dimension decision provenance;
- completed Assessment History, URL pagination and canonical Session detail navigation;
- Vitest / React Testing Library / MSW coverage plus repeated real-browser integration verification.

Still deferred in the frontend are real provider-backed clarification interaction, Group/Sharing UI and historical deletion UI. The project is now intentionally pausing frontend feature development while the AWS/CI-CD line is implemented.

---

## Documentation

The repository treats architecture and implementation decisions as first-class project artifacts rather than keeping them only in code comments.

```text
docs/
├── README.md                 Documentation entry point
├── requirements.md           Product goals, MVP scope and privacy rules
├── architecture.md           System and module architecture baseline
├── roadmap.md                Current progress and remaining work
├── domain/                   Assessment and Group domain design
├── api/
│   └── openapi.yaml          Machine-readable HTTP contract
├── adr/                      Architecture Decision Records
├── backend/                  Detailed backend design and implementation checkpoints
└── frontend/                 Frontend architecture + F1-F6 implementation checkpoint
```

Recommended starting points:

- [`docs/README.md`](docs/README.md) — documentation index;
- [`docs/architecture.md`](docs/architecture.md) — architecture and system boundaries;
- [`docs/domain/assessment-domain.md`](docs/domain/assessment-domain.md) — Assessment lifecycle and invariants;
- [`docs/domain/group-spec-aligned.md`](docs/domain/group-spec-aligned.md) — Group, membership and sharing rules;
- [`docs/api/openapi.yaml`](docs/api/openapi.yaml) — current OpenAPI contract;
- [`docs/adr/`](docs/adr/) — major design decisions and trade-offs;
- [`docs/backend/15-assessment-clarification-workflow-checkpoint.md`](docs/backend/15-assessment-clarification-workflow-checkpoint.md) — latest accepted backend checkpoint;
- [`docs/frontend/README.md`](docs/frontend/README.md) — frontend architecture/implementation index.
- [`docs/frontend/05-implementation-checkpoint-f1-f6.md`](docs/frontend/05-implementation-checkpoint-f1-f6.md) — current F1-F6 implementation checkpoint and Cloud handoff.

---

## Testing Strategy

The project uses different test levels for different responsibilities:

- **Backend Domain tests** — business rules and state transitions without Spring or database dependencies;
- **Backend Application/Web MVC tests** — use-case orchestration, HTTP/security and Controller contracts;
- **PostgreSQL integration tests** — JPA mappings, Flyway schema, constraints, JSONB and query behavior;
- **Concurrency tests** — locking, uniqueness and retry/recovery behavior on critical workflows;
- **Architecture tests** — package/module dependency guardrails;
- **Frontend unit/integration tests** — Vitest + React Testing Library + MSW for browser behavior at the HTTP boundary;
- **Real-browser verification** — manual Browser -> Vite -> Spring Boot -> PostgreSQL checks for the implemented vertical slice.

Run the backend suite with:

```bash
cd backend
./mvnw test
```

Run the frontend quality gates with:

```bash
cd frontend
npm run test
npm run build
npm run lint
```

Dedicated Playwright E2E automation remains deferred until CI/CD provides a stable repeatable full-stack environment.

---

## Running Locally

### Prerequisites

- JDK 21;
- Node.js 24 LTS;
- Docker + Docker Compose;
- Internet access on the first Maven Wrapper / npm dependency install.

### 1. Start PostgreSQL

From the repository root:

```bash
docker compose up -d postgres
```

Check container health:

```bash
docker compose ps
```

### 2. Run the backend

```bash
cd backend
./mvnw spring-boot:run
```

Then verify the application:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

### 3. Run the frontend

In a second terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open:

```text
http://localhost:5173
```

Vite proxies relative `/api` and `/actuator` requests to the backend on port `8080`.

### 4. Run tests / build checks

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm run test
npm run build
npm run lint
```

---

## Database Ownership & Migration History

**Flyway owns the database schema.** Hibernate validates the mapped model but does not create or mutate production schema:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Current migration history:

```text
V1__create_identity_tables.sql
V2__create_spring_session_tables.sql
V3__create_assessment_tables.sql
V4__seed_sixteen_personality_v1.sql
V5__add_clarification_execution_token.sql
```

Applied migrations are treated as immutable history and are not renumbered to match older design documents. Future database work begins at `V6` or later.

---

## Repository Structure

```text
spring-aws-portfolio/
├── backend/                  Java 21 + Spring Boot application
├── frontend/                 React + TypeScript + Vite application
├── docs/                     Requirements, architecture, ADRs and checkpoints
├── infra/
│   └── terraform/            Terraform workspace for the current AWS phase
├── compose.yaml              Local PostgreSQL environment
├── .env.example              Local configuration example
└── README.md
```

The deterministic full-stack Assessment slice is runnable locally. The current delivery phase is turning that local application into a reproducible containerized AWS deployment with Terraform and GitHub Actions.

---

## Design Principles

A few principles guide the project as it evolves:

- **Business rules belong on the backend**, not in frontend assumptions.
- **Durable invariants must survive restarts and concurrency**, so important constraints are enforced in PostgreSQL as well as application/domain logic where appropriate.
- **AI is optional and bounded**; deterministic evidence remains authoritative and explainable.
- **Privacy is explicit**; Group membership never implies consent to share assessment data.
- **Architecture decisions should be explainable**; important trade-offs are recorded as ADRs.
- **Avoid premature complexity**; the MVP uses a modular monolith instead of microservices.
- **Implementation feedback may change the design**, but significant changes are documented instead of silently rewriting architectural history.

---

## Project Goal

This repository is a personal learning and career-transition portfolio project. The goal is not only to make the application work, but to be able to explain the engineering decisions behind it in an interview:

- why the domain is modeled this way;
- where transaction and concurrency boundaries belong;
- why PostgreSQL is used as part of invariant enforcement;
- why the project uses server-side sessions instead of JWT for the MVP;
- how deterministic logic is separated from AI-assisted behavior;
- how testing strategy changes with the type of risk being verified;
- how the already-runnable application is containerized, deployed to AWS, automated with GitHub Actions, and provisioned with Terraform.

The final target is a runnable full-stack application with documented architecture, automated tests, CI/CD, AWS deployment, and reproducible infrastructure.
