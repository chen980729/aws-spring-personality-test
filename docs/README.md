# Documentation Index

This directory contains the Architecture/Domain baselines, implementation-facing backend design, machine-readable API contract, and implementation checkpoints for the Spring AWS Portfolio Project.

## Current status

The backend implementation is aligned through **Assessment Step 7 — Clarification + Tie-break workflow boundaries**. Identity/Security, the deterministic Assessment core/read side, Clarification lifecycle persistence, Skip/Tie-break finalization, stale external-result protection, and deterministic Clarification/Tie-break HTTP endpoints are implemented. Frontend **F0 — Integration Baseline + Architecture** is also accepted. Real LLM interaction (Step 8), frontend implementation (F1+), historical deletion orchestration, Group, Cloud and CI/CD work remain.

Use these documents as the main entry points:

- `requirements.md` — product goals, MVP scope, privacy, and Definition of Done.
- `architecture.md` — system/module boundaries and system-level architectural constraints.
- `domain/assessment-domain.md` — current Assessment lifecycle, Aggregate boundaries, invariants, provenance, and deletion semantics.
- `domain/group-spec-aligned.md` — accepted Group lifecycle, memberships, join requests, admin authority, and sharing consent.
- `sixteen-personality-spec-aligned.md` — executable Sixteen Personality Assessment specification.
- `backend/README.md` — backend design + implementation-checkpoint index.
- `frontend/README.md` — accepted Frontend F0 architecture/integration baseline and implementation handoff.
- `api/openapi.yaml` — OpenAPI **v0.4.0** HTTP contract.
- `roadmap.md` — current implementation progress and remaining workstreams.
- `adr/` — Architecture Decision Records, including superseded decisions where implementation feedback changed the design.

Detailed backend/database/API/AWS design should refine this baseline rather than silently contradict it. When implementation feedback changes a major architectural decision, record the change explicitly in an ADR.

## Backend API contract

- `backend/06-rest-api-contract.md` — REST resource/action semantics and recovery rules.
- `backend/08-authentication-security.md` — accepted MVP Authentication & Security design.
- `api/openapi.yaml` — OpenAPI v0.4.0 design-first HTTP DTO/security contract.
- `api/README.md` — API contract ownership and version rationale.

## Frontend F0 baseline

- `frontend/README.md` — F0 checkpoint/index.
- `frontend/01-frontend-architecture.md` — routing/page boundaries, state ownership, feature/project structure, and canonical Assessment Session route.
- `frontend/02-api-integration.md` — executable API baseline, target-vs-runtime contract rule, query/mutation ownership and recovery behavior.
- `frontend/03-auth-session-csrf.md` — Session/CSRF browser protocol and same-origin development topology.
- `frontend/04-testing-strategy.md` — Vitest/RTL/MSW/Playwright responsibility split and E2E boundary.

## Latest backend checkpoints

- `backend/09-identity-authentication-implementation-checkpoint.md` — Identity/Auth implementation checkpoint.
- `backend/11-assessment-session-read-checkpoint.md` — Assessment read/start/resume checkpoint.
- `backend/12-assessment-submission-scoring-checkpoint.md` — submission/scoring checkpoint.
- `backend/13-assessment-restart-checkpoint.md` — Restart / Start New checkpoint.
- `backend/14-assessment-history-detail-checkpoint.md` — Assessment Step 6 History / Historical Detail checkpoint.
- `backend/15-assessment-clarification-workflow-checkpoint.md` — latest accepted checkpoint: Assessment Step 7 Clarification workflow.
