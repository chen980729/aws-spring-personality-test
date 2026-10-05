# Backend Documentation Index

> **Status:** Active backend design + implementation record
> **Last updated:** 2026-10-05
> **Current implementation checkpoint:** DefinitionVersion 1.1 Step 2D — version-aware Tie-break finalization compatibility implemented; activation/Frontend integration pending
> **Current project focus:** AWS deployment / containerization / Terraform / CI/CD
> **Deferred backend feature work:** Group/Sharing; historical deletion after Group sharing; provider-backed Step 8 LLM runtime after the Cloud/CI-CD line

This directory contains two kinds of documents:

1. **Design baseline** — the intended backend architecture and contracts.
2. **Implementation checkpoints** — what has actually been implemented, tested, and accepted so far.

Implementation feedback may refine the original baseline. Significant boundary changes must be recorded explicitly in ADRs rather than silently rewriting history.

Domain truth remains primarily in:

- `docs/domain/assessment-domain.md`
- `docs/domain/group-spec-aligned.md`
- `docs/sixteen-personality-spec-aligned.md`

## Design documents

1. `01-module-package-boundary.md` — logical modules, package ownership, public module contracts, orchestration boundary.
2. `02-application-use-case-boundary.md` — commands, queries, transaction ownership, authorization, Clarification coordination, and cross-module workflows.
3. `03-spring-layer-interaction.md` — Controller/Application/Domain/Infrastructure dependency direction, Repository abstraction, Domain/JPA separation, time handling.
4. `04-persistence-postgresql-design.md` — relational/JSONB strategy, invariants, concurrency, locking, deletion and index strategy.
5. `05-flyway-jpa-mapping.md` — actual Flyway migration history, table/constraint design, JPA mapping rules, repository adapters and persistence testing.
6. `06-rest-api-contract.md` — `/api/v1` resource/action semantics, retry/recovery protocol, RFC 9457 error policy and privacy boundary.
7. `07-implementation-handoff.md` — original design-to-implementation handoff, now annotated with the current implementation state.
8. `08-authentication-security.md` — server-side Session/JDBC, cookie, CSRF, password and Spring Security boundary design.
10. `10-backend-testing-strategy.md` — active testing strategy and coverage through Assessment Step 7.

Machine-readable API contract: `../api/openapi.yaml` (**contract version v0.5.0**). Backend GET/PUT, Session tie-break projections, and final decision-source provenance now implement the version-aware 1.0/1.1 contract. DefinitionVersion 1.1 remains DRAFT until later activation and Frontend integration.

## Implementation checkpoints

9. `09-identity-authentication-implementation-checkpoint.md` — Register / Login / Session / CSRF / `/me` / Logout.
11. `11-assessment-session-read-checkpoint.md` — catalog, Start/Resume, active Session and bound questionnaire reads.
12. `12-assessment-submission-scoring-checkpoint.md` — autosave, Submit, deterministic scoring, ambiguity and immediate completion.
13. `13-assessment-restart-checkpoint.md` — Restart / Start New, locking, concurrency and recovery semantics.
14. `14-assessment-history-detail-checkpoint.md` — completed History + Historical Detail and authoritative persisted workflow reads.
15. `15-assessment-clarification-workflow-checkpoint.md` — Clarification Aggregate lifecycle, deterministic finalization, stale-result protection and deterministic HTTP boundary.

## High-level backend shape

```text
HTTP
  ↓
Web
  ↓
Application / Orchestration
  ↓
Domain
  ↓
Repository / Output Port abstractions
  ↑
Infrastructure adapters
  ↓
PostgreSQL / external LLM
```

The MVP remains one Java 21 Spring Boot deployable and one build module. Modularity is enforced through package boundaries, narrow public contracts and architecture tests rather than Maven/Gradle multi-module decomposition.

## Current accepted implementation state

Authentication & Security is implemented around:

- server-side Spring Security Session.
- Spring Session JDBC in PostgreSQL, with Flyway-owned session schema and runtime auto-init disabled.
- production `__Host-SESSION` Secure/HttpOnly/SameSite=Lax cookie policy.
- CSRF enabled with `GET /api/v1/auth/csrf` + `X-CSRF-TOKEN`.
- Argon2id password hashing behind an Infrastructure adapter.
- Spring Security establishes identity; Application/Domain owns business authorization.

Assessment includes the completed Step 7 baseline plus the in-progress DefinitionVersion 1.1 compatibility slice:

```text
Catalog
-> Start / Resume
-> Session-bound Questionnaire
-> Autosave
-> Submit
-> deterministic Scoring / Ambiguity
-> immediate Finalization when possible
-> Restart / Start New
-> History / Historical Detail
-> Clarification Aggregate lifecycle
-> Skip one / Skip remaining
-> explicit exact-tie decision
-> deterministic post-clarification Finalization
-> durable external-execution boundary with stale-result protection
-> deterministic Clarification/Tie-break HTTP endpoints
-> version-bound Tie-break GET interaction
-> exclusive legacy/contextual Tie-break PUT validation
-> contextual question/option provenance persistence and Session projection
-> provenance-driven final source: USER_TIE_BREAK vs TIE_BREAK_QUESTION
```

`ADR-0016` supersedes the original in-memory Aggregate choice from `ADR-0007`: `DimensionClarification` is a separate Assessment Aggregate coordinated with `AssessmentSession` using short Session locks, local transactions, durable uniqueness constraints and an opaque active-execution correlation token while external work is in flight.

The real LLM provider/runtime interaction is still deferred to Step 8. In particular, the public Start / Continue / Retry interaction endpoints are design-frozen but are not yet wired to a real provider. The deterministic Skip and Tie-break HTTP capabilities are implemented, tested and consumed by the frontend.

Historical Assessment deletion also remains intentionally unimplemented. Its contract is designed, but the real use case must coordinate with Group sharing before hard deletion; it should not be implemented as an Assessment-only shortcut before the Group sharing boundary exists.

The project is currently shifting from feature implementation to Cloud/Delivery. Backend work in the next phase should therefore include production/deployment configuration and CI verification without prematurely reopening the deferred LLM/Group/deletion feature boundaries.

Further design changes should continue to be driven by explicit implementation feedback and recorded in the relevant design document and/or ADR.
