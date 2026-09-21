# Backend Detailed Design Index

> **Status:** Backend Detailed Design v1.0 — Accepted / Ready for Implementation  
> **Last updated:** 2026-09-21  
> **Next phase:** Implementation — project skeleton and architecture guardrails

This directory refines the system-level baseline in `docs/architecture.md` into implementation-facing Spring Boot backend decisions.

The documents here do **not** redefine the Assessment or Group business rules. Domain truth remains in:

- `docs/domain/assessment-domain.md`
- `docs/domain/group-spec-aligned.md`
- `docs/sixteen-personality-spec-aligned.md`

## Frozen design documents

1. `01-module-package-boundary.md` — logical modules, package ownership, public module contracts, orchestration boundary.
2. `02-application-use-case-boundary.md` — commands, queries, transaction ownership, business authorization and cross-module workflows.
3. `03-spring-layer-interaction.md` — Controller/Application/Domain/Infrastructure dependency direction, Repository abstraction, Domain/JPA separation and time handling.
4. `04-persistence-postgresql-design.md` — relational/JSONB strategy, invariants, concurrency, locking, hard deletion and index strategy.
5. `05-flyway-jpa-mapping.md` — migration layout, concrete table/constraint design, JPA mapping rules, repository adapters and persistence testing.
6. `06-rest-api-contract.md` — accepted `/api/v1` resource/action semantics, retry/recovery protocol, RFC 9457 error policy and privacy boundary.
7. `07-implementation-handoff.md` — frozen-vs-open status and recommended implementation order before coding begins.
8. `08-authentication-security.md` — accepted server-side Session/JDBC, cookie, CSRF, password and Spring Security boundary design.

Machine-readable API contract: `../api/openapi.yaml` (contract version v0.2.0).

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

The MVP remains one Java 21 Spring Boot deployable and one build module. Modularity is enforced first through package boundaries, narrow public contracts and architecture tests rather than Maven/Gradle multi-module decomposition.

## Final acceptance status

Authentication & Security is now frozen for the MVP:

- server-side Spring Security Session.
- Spring Session JDBC in PostgreSQL, with Flyway-owned session schema and runtime auto-init disabled.
- production `__Host-SESSION` Secure/HttpOnly/SameSite=Lax cookie.
- CSRF enabled with `GET /api/v1/auth/csrf` + `X-CSRF-TOKEN`.
- Argon2id password hashing behind an Infrastructure adapter.
- same-origin deployment preferred; no wildcard credentialed CORS.
- Spring Security establishes identity; Application/Domain keeps business authorization.

`docs/api/openapi.yaml` contains the concrete OpenAPI v0.2.0 DTO/security contract. The contract and Backend Detailed Design v1.0 are **Accepted / Ready for Implementation**.

## Final-alignment decisions applied after review

The backend documents now explicitly include:

- `StartAssessment` = create-if-none/resume-existing, plus separate atomic `RestartAssessmentSession` / Start New.
- `ABANDONED` allowed from all non-terminal active AssessmentSession states.
- explicit `RetryDimensionClarification` for `FAILED_RETRYABLE`.
- at most one `AVAILABLE AssessmentDefinitionVersion` per AssessmentDefinition.
- Session-bound questionnaire GET always renders the exact bound version.
- `UNCLEAR` is an accepted clarification business result with no suggested pole; technical failure remains `FAILED_RETRYABLE`.
- Identity owns `displayName`; Group read models use it instead of exposing email.
- atomic Submit + deterministic post-processing; no normal persisted submitted-but-unscored state.
- client recovery after lost Submit/Restart responses.
- deferred account-deletion impact documented.

This directory is now marked **Backend Detailed Design v1.0 — Accepted / Ready for Implementation**. Further design changes should be made deliberately through implementation feedback or an explicit design/ADR update rather than by silently changing the frozen baseline.
