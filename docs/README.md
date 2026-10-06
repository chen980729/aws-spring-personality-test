# Documentation Index

This directory contains the Architecture/Domain baselines, implementation-facing backend design, machine-readable API contract, and implementation checkpoints for the Spring AWS Portfolio Project.

## Current status

The backend implementation is aligned through **Assessment Step 7 plus DefinitionVersion 1.1 activation**. Identity/Security, the deterministic Assessment core/read side, Clarification lifecycle persistence, version-aware Tie-break interaction/finalization, stale external-result protection, contextual provenance persistence/history, the v0.5.0 HTTP boundary, and the V8 availability promotion are implemented.

Frontend **F1-F6 plus F7-A/F7-B/F7-C/F7-D and the F7-E final review** is complete for the executable Authentication + deterministic Assessment slice: Register/Login/Session/CSRF, Assessment entry, questionnaire/autosave/submit, deterministic clarification, dual-version Tie-break compatibility, enriched Result interpretation, completed History and canonical Session navigation. The Result page keeps backend-authoritative evidence/decision provenance while adding a separate Content Version 1.0 catalog and optimized per-type illustrations; the public Landing page now uses the optimized four-profile hero artwork. Provider-backed LLM interaction, Group UI and historical deletion remain intentionally deferred because their executable backend boundaries are not complete.

The project has completed its AWS deployment and delivery foundation: Docker, Terraform, RDS, ECR, ECS Fargate, ALB, private S3 + CloudFront, CloudWatch, Secrets Manager, GitHub Actions CI, GitHub OIDC and least-privilege CD are implemented. Manual CD and the automatic `main CI success -> CD` path are both verified end-to-end, including backend/frontend deployment and the post-deployment public smoke test. DefinitionVersion 1.1 is now active for new Sessions while retained 1.0 Sessions remain executable. Provider-backed LLM clarification is the next product milestone; historical Assessment deletion still waits for the Group sharing boundary.

Use these documents as the main entry points:

- `requirements.md` — product goals, MVP scope, privacy, and Definition of Done.
- `architecture.md` — system/module boundaries and system-level architectural constraints.
- `domain/assessment-domain.md` — current Assessment lifecycle, Aggregate boundaries, invariants, provenance, and deletion semantics.
- `domain/group-spec-aligned.md` — accepted Group lifecycle, memberships, join requests, admin authority, and sharing consent.
- `sixteen-personality-spec-aligned.md` — executable Sixteen Personality Assessment specification.
- `backend/README.md` — backend design + implementation-checkpoint index.
- `frontend/README.md` — frontend documentation index, current F7-E review checkpoint and deferred boundaries.
- `frontend/05-implementation-checkpoint-f1-f6.md` — accepted F1-F6 implementation checkpoint, post-F6 UI optimization checkpoint and cloud handoff.
- `frontend/06-result-content-checkpoint-f7c.md` — F7-C result interpretation/content/image checkpoint.
- `frontend/07-landing-hero-checkpoint-f7d.md` — F7-D Landing hero visual/performance/accessibility checkpoint.
- `frontend/08-final-review-checkpoint-f7e.md` — F7-E final review, compatibility-release decision and activation sequencing checkpoint.
- `api/openapi.yaml` — OpenAPI **v0.5.0** contract; DefinitionVersion 1.1 Backend/Frontend dual-version support and activation regression coverage are implemented, with V8 promoting 1.1 for new Sessions.
- `roadmap.md` — current implementation progress and remaining workstreams.
- `deployment/aws-deployment.md` — as-built AWS topology, networking, cost/security trade-offs and rollout tuning.
- `deployment/ci-cd.md` — CI/CD flow, OIDC security, least-privilege deployment and release ownership.
- `future-work.md` — explicit future options and deferred hardening/product work.
- `adr/` — Architecture Decision Records, including superseded decisions where implementation feedback changed the design.
- `adr/ADR-0017-terraform-cd-ownership.md` — Terraform infrastructure ownership vs application CD revision ownership.
- `adr/ADR-0018-contextual-tie-break-definition-version.md` — immutable 1.0 compatibility and DefinitionVersion 1.1 contextual tie-break decision.

Detailed backend/database/API/AWS design should refine this baseline rather than silently contradict it. When implementation feedback changes a major architectural decision, record the change explicitly in an ADR.

## Backend API contract

- `backend/06-rest-api-contract.md` — REST resource/action semantics and recovery rules.
- `backend/08-authentication-security.md` — accepted MVP Authentication & Security design.
- `api/openapi.yaml` — OpenAPI v0.5.0 design-first HTTP DTO/security contract; Backend runtime implements both 1.0/1.1 tie-break semantics, and V8 makes 1.1 the version used for new Sessions.
- `api/README.md` — API contract ownership and version rationale.

## Frontend architecture and implementation

- `frontend/README.md` — current frontend documentation index.
- `frontend/01-frontend-architecture.md` — F0 routing/page/state baseline, now validated through the F7-E review and the post-F6 presentation refactor.
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
- `backend/15-assessment-clarification-workflow-checkpoint.md` — historical Assessment Step 7 Clarification workflow checkpoint.
- `backend/16-contextual-tie-break-compatibility-checkpoint.md` — DefinitionVersion 1.1 compatibility + Step 2E pre-activation acceptance.
- `backend/17-definition-version-1-1-activation-checkpoint.md` — latest Backend checkpoint: V8 activation + post-activation Session-binding coverage.


## Cloud / Delivery checkpoint

The repository now has a real deployed delivery baseline rather than only a target design.

- AWS region: `ap-northeast-1`.
- Browser entry: CloudFront.
- Frontend: private S3 through OAC.
- Backend: ALB → ECS Fargate Spring Boot.
- Database: private RDS PostgreSQL.
- Container registry: ECR with immutable image tags.
- Runtime secrets: Secrets Manager.
- Logs: CloudWatch Logs.
- Infrastructure: Terraform with remote S3 state.
- CI: backend/frontend quality gates plus Docker verification.
- AWS authentication: GitHub OIDC → STS temporary credentials.
- CD: least-privilege backend + frontend deployment.
- Verified: manual CD and automatic successful-`main`-CI → CD, including backend/frontend deployment and the public post-deployment smoke test.

The canonical architecture diagram appears before the CI/CD flow diagram in `architecture.md`, and both are also surfaced from the repository README for portfolio review.
