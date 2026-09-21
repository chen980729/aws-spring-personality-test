# Documentation Index

This directory contains the frozen Architecture/Domain baselines and the current accepted or in-progress detailed design for the Spring AWS Portfolio Project.

- `requirements.md` — product goals, final MVP scope, privacy, and Definition of Done.
- `architecture.md` — system/module boundaries, persistence principles, backend capability boundary, recovery/idempotency principles, and deferred detailed design.
- `domain/assessment-domain.md` — Assessment lifecycle, domain model, aggregates, invariants, provenance, and deletion semantics.
- `domain/group-spec-aligned.md` — Group lifecycle, memberships, join requests, admin authority, sharing consent, aggregates and invariants.
- `sixteen-personality-spec-aligned.md` — executable Sixteen Personality Assessment specification.
- `backend/` — accepted Spring Boot module/application/persistence/REST/Security design. OpenAPI v0.2.0 is accepted; Backend Detailed Design v1.0 is ready for implementation. Start with `backend/README.md`.
- `roadmap.md` — workstreams from executable specification through implementation, AWS, CI/CD, and portfolio finalization.
- `adr/` — accepted Architecture Decision Records.

Detailed backend/database/API/AWS design should refine this baseline rather than silently contradict it.

## Backend API contract

- `backend/08-authentication-security.md` — accepted MVP Authentication & Security design.
- `api/openapi.yaml` — OpenAPI v0.2.0 exact HTTP DTO/security contract (accepted for implementation).
- `api/README.md` — API contract ownership and version rationale.
