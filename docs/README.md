# Documentation Index

This directory contains the Architecture/Domain baselines, implementation-facing backend design, machine-readable API contract, and implementation checkpoints for the Spring AWS Portfolio Project.

## Current status

The backend implementation is aligned through **Assessment Step 7 — Clarification + Tie-break workflow boundaries**. Identity/Security, the deterministic Assessment core/read side, Clarification lifecycle persistence, Skip/Tie-break finalization, stale external-result protection, and deterministic Clarification/Tie-break HTTP endpoints are implemented.

Frontend **F1-F6** is implemented for the executable Authentication + deterministic Assessment slice: Register/Login/Session/CSRF, Assessment entry, questionnaire/autosave/submit, deterministic clarification Skip/Tie-break, Result, completed History and canonical Session navigation. A post-F6 UI optimization milestone is also complete, adding the lightweight design-token/App-Shell/shared-UI presentation layer, responsive behavior and polished loading/error/empty/404 states without changing feature contracts. Provider-backed LLM interaction, Group UI and historical deletion remain intentionally deferred because their executable backend boundaries are not complete.

The project has now paused additional frontend feature work and moved its active focus to **containerization, AWS deployment, Terraform and GitHub Actions CI/CD**. Historical Assessment deletion resumes after the Group sharing boundary exists; provider-backed LLM Step 8 is planned after the Cloud/CI-CD line is established.

Use these documents as the main entry points:

- `requirements.md` — product goals, MVP scope, privacy, and Definition of Done.
- `architecture.md` — system/module boundaries and system-level architectural constraints.
- `domain/assessment-domain.md` — current Assessment lifecycle, Aggregate boundaries, invariants, provenance, and deletion semantics.
- `domain/group-spec-aligned.md` — accepted Group lifecycle, memberships, join requests, admin authority, and sharing consent.
- `sixteen-personality-spec-aligned.md` — executable Sixteen Personality Assessment specification.
- `backend/README.md` — backend design + implementation-checkpoint index.
- `frontend/README.md` — frontend documentation index, current F6 + UI optimization checkpoint and deferred boundaries.
- `frontend/05-implementation-checkpoint-f1-f6.md` — accepted F1-F6 implementation checkpoint, post-F6 UI optimization checkpoint and cloud handoff.
- `api/openapi.yaml` — OpenAPI **v0.4.0** HTTP contract.
- `roadmap.md` — current implementation progress and remaining workstreams.
- `adr/` — Architecture Decision Records, including superseded decisions where implementation feedback changed the design.

Detailed backend/database/API/AWS design should refine this baseline rather than silently contradict it. When implementation feedback changes a major architectural decision, record the change explicitly in an ADR.

## Backend API contract

- `backend/06-rest-api-contract.md` — REST resource/action semantics and recovery rules.
- `backend/08-authentication-security.md` — accepted MVP Authentication & Security design.
- `api/openapi.yaml` — OpenAPI v0.4.0 design-first HTTP DTO/security contract.
- `api/README.md` — API contract ownership and version rationale.

## Frontend architecture and implementation

- `frontend/README.md` — current frontend documentation index.
- `frontend/01-frontend-architecture.md` — F0 routing/page/state baseline, now validated through F6 and the post-F6 presentation refactor.
- `frontend/02-api-integration.md` — executable API baseline, query/mutation ownership and implemented recovery behavior.
- `frontend/03-auth-session-csrf.md` — Session/CSRF browser protocol and implemented authentication recovery.
- `frontend/04-testing-strategy.md` — Vitest/RTL/MSW strategy, current coverage, UI regression lesson and deferred Playwright boundary.
- `frontend/05-implementation-checkpoint-f1-f6.md` — implemented F1-F6 browser flow, integration findings, remaining scope and AWS/CI-CD handoff.

## Latest backend checkpoints

- `backend/09-identity-authentication-implementation-checkpoint.md` — Identity/Auth implementation checkpoint.
- `backend/11-assessment-session-read-checkpoint.md` — Assessment read/start/resume checkpoint.
- `backend/12-assessment-submission-scoring-checkpoint.md` — submission/scoring checkpoint.
- `backend/13-assessment-restart-checkpoint.md` — Restart / Start New checkpoint.
- `backend/14-assessment-history-detail-checkpoint.md` — Assessment Step 6 History / Historical Detail checkpoint.
- `backend/15-assessment-clarification-workflow-checkpoint.md` — latest accepted checkpoint: Assessment Step 7 Clarification workflow.
