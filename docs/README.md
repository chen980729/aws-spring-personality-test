# Documentation Index

This directory contains the Architecture/Domain baselines, implementation-facing backend design, machine-readable API contract, and implementation checkpoints for the Spring AWS Portfolio Project.

## Current status

The backend implementation is aligned through **Assessment Step 6 — History + Historical Detail**. Identity/Security and the deterministic Assessment core/read side are implemented; Clarification/Tie-break mutation, deletion, Group, Frontend, Cloud and CI/CD work remain.

Use these documents as the main entry points:

- `requirements.md` — product goals, MVP scope, privacy, and Definition of Done.
- `architecture.md` — system/module boundaries and system-level architectural constraints.
- `domain/assessment-domain.md` — current Assessment lifecycle, Aggregate boundaries, invariants, provenance, and deletion semantics.
- `domain/group-spec-aligned.md` — accepted Group lifecycle, memberships, join requests, admin authority, and sharing consent.
- `sixteen-personality-spec-aligned.md` — executable Sixteen Personality Assessment specification.
- `backend/README.md` — backend design + implementation-checkpoint index.
- `api/openapi.yaml` — OpenAPI **v0.3.0** HTTP contract.
- `roadmap.md` — current implementation progress and remaining workstreams.
- `adr/` — Architecture Decision Records, including superseded decisions where implementation feedback changed the design.

Detailed backend/database/API/AWS design should refine this baseline rather than silently contradict it. When implementation feedback changes a major architectural decision, record the change explicitly in an ADR.

## Backend API contract

- `backend/06-rest-api-contract.md` — REST resource/action semantics and recovery rules.
- `backend/08-authentication-security.md` — accepted MVP Authentication & Security design.
- `api/openapi.yaml` — OpenAPI v0.3.0 exact HTTP DTO/security contract.
- `api/README.md` — API contract ownership and version rationale.

## Latest backend checkpoints

- `backend/09-identity-authentication-implementation-checkpoint.md` — Identity/Auth implementation checkpoint.
- `backend/11-assessment-session-read-checkpoint.md` — Assessment read/start/resume checkpoint.
- `backend/12-assessment-submission-scoring-checkpoint.md` — submission/scoring checkpoint.
- `backend/13-assessment-restart-checkpoint.md` — Restart / Start New checkpoint.
- `backend/14-assessment-history-detail-checkpoint.md` — latest accepted checkpoint: Assessment Step 6.
