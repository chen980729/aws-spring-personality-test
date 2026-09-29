# Backend Design Status & Implementation Handoff

> **Status:** Historical design-to-implementation handoff; implementation has progressed through Assessment Step 7
> **Last updated:** 2026-09-30

## 1. What is frozen enough to implement

### Domain

Assessment and Group Domain Models are the business source of truth.

### Module structure

- Modular Monolith.
- single Spring Boot build/deployable.
- package-by-business-module.
- narrow `api` contracts.
- explicit cross-module `orchestration`.

### Application layer

- command/query responsibility separation without full CQRS infrastructure.
- use-case-oriented mutation services.
- explicit business authorization.
- transaction boundary follows consistency boundary.
- no long DB transaction across LLM calls.

### Persistence

- PostgreSQL + Flyway + JPA/Hibernate.
- Domain/JPA model separation.
- JSONB for bounded Assessment snapshots.
- relational lifecycle rows where independently constrained.
- hard-delete historical Assessment.
- no FK from Group Share to AssessmentSession.
- partial unique indexes for conditional uniqueness.
- optimistic `@Version` plus targeted `FOR UPDATE` locks.
- Testcontainers PostgreSQL for persistence/concurrency tests.

### HTTP

- `/api/v1` JSON API resource/action semantics.
- explicit lifecycle actions, including Start New/Restart and Clarification Retry.
- current actor comes from authentication context.
- RFC 9457 Problem Details + stable application `code`.
- explicit client recovery for lost Submit/Restart responses.
- owner-vs-shared Assessment privacy representations.
- exact DTO/security schemas are published in `docs/api/openapi.yaml` (current design contract v0.4.0).

## 2. Final review + intentionally deferred slice decisions

### Backend v1.0 status

`docs/api/openapi.yaml` contains the design-first DTO/security contract, including Session-bound questionnaire retrieval, clarification `UNCLEAR` semantics, Group display identity, and authentication/CSRF behavior. The design baseline remains accepted; the repository has since implemented Identity/Security and Assessment through Step 7. Deterministic Skip/Tie-break routes are now implemented; provider-backed Start/Continue/Retry interaction remains Step 8 work.

### Deferred slice-specific decisions that need not block skeleton creation

- JoinRequest expiration execution: correctness must be lazy (`now >= expiresAt` on relevant operations); scheduled cleanup may be added to normalize rows promptly. Freeze concrete scheduling when implementing Group.
- AI runtime context ownership/storage, provider retry/timeout and structured-output parsing. Freeze these before implementing the real AI adapter; a fake adapter may be used earlier.

## 3. Recommended implementation order

### Phase 1 — project skeleton and architecture guardrails

1. Java 21 / Spring Boot 4.1.1 / Maven project dependencies.
2. package skeleton rooted at `dev.springawsportfolio.portfolio`.
3. ArchUnit module/layer rules.
4. test profiles / Testcontainers base setup.

### Phase 2 — persistence foundation

1. Flyway V1-V5 (current applied history).
2. JPA entities.
3. persistence mappers/adapters.
4. constraint/repository integration tests.
5. Assessment reference-data seed in V4.

These foundation items are now implemented; future migrations continue from V6 without renumbering applied versions.

### Phase 3 — Identity/Security foundation

1. UserAccount Domain/Application/Persistence.
2. password hashing.
3. chosen authentication transport.
4. SecurityContext -> `UserId` adapter.
5. RFC 9457 authentication/authorization errors.

### Phase 4 — Assessment vertical slice

Implement one end-to-end path first:

```text
List Assessment
-> Start / Resume Session
-> Restart (Start New)
-> Save questionnaire
-> Submit questionnaire
-> deterministic result
-> history
```

Clarification/tie-break behavior is now implemented through the deterministic HTTP boundary, including lifecycle persistence, Skip, exact-tie handling, deterministic finalization and stale external-result protection. The real provider adapter/runtime interaction is the next Assessment slice.

### Phase 5 — Group vertical slice

```text
Create Group
-> JoinRequest
-> approve/reject
-> membership/admin
-> leave/disband
```

### Phase 6 — cross-module sharing

```text
Start/change/stop share
-> shared-result queries
-> Assessment deletion orchestration
```

### Phase 7 — concurrency and recovery proof

Implement focused tests for:

- concurrent StartAssessment.
- concurrent JoinRequest.
- transfer Admin vs Leave.
- approve vs reject/expire.
- concurrent clarification start/finalization.
- sharing vs Assessment deletion.

## 4. Definition of ready for coding

Backend architecture is ready for substantial implementation when:

```text
Domain rules are accepted
Module boundaries are accepted
Application use cases are accepted
Persistence model/constraints are accepted
REST resource/action semantics are accepted
Authentication/Security design is accepted
OpenAPI exact DTO contract is accepted
```

At the current repository checkpoint, Authentication/Security and Assessment Steps 1-7 are implemented and tested. OpenAPI v0.4.0 is the active design-first DTO contract. Step 8 real LLM integration is the next Assessment implementation focus.

## 5. Implementation principle for Codex

Codex may perform repository analysis, coding, tests and refactoring, but should treat the accepted design documents as constraints rather than silently redesigning boundaries.

If implementation reveals a contradiction or materially better trade-off, surface it as a design issue first. Significant changes to Aggregate boundaries, API semantics, transaction ownership, privacy, deletion semantics or persistence guarantees should be recorded in docs/ADR rather than introduced as incidental code refactors.
