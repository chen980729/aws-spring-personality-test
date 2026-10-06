# Project Roadmap

## 1. Roadmap Principle

Phase numbering represents **logical workstreams and dependency order where relevant**, not a requirement that all work be performed strictly sequentially. Once architecture and interface contracts are stable enough, Backend, Frontend, AI, UI/UX, Cloud, and CI/CD work can partially overlap and iterate.

The final MVP includes Group and historical assessment deletion, even if the core assessment vertical slice is implemented first.

## 2. Completed: Product and Architecture Baseline

- [x] Product definition.
- [x] Product requirements.
- [x] Assessment lifecycle.
- [x] Assessment domain model.
- [x] Entity / Value Object classification.
- [x] Aggregate / Aggregate Root design.
- [x] Domain invariants.
- [x] Versioning and provenance direction.
- [x] Logical module boundary.
- [x] Conceptual data model / persistence requirements.
- [x] Backend business capability boundary.
- [x] Architecture consolidation and ADR baseline.
- [x] Architecture Baseline Final Review.

## 3. Completed: Sixteen Personality Assessment Specification

**Completed before production scoring implementation.**

Accepted artifact:

`docs/sixteen-personality-spec-aligned.md`

Topics to define without inventing unsupported psychology claims:

- [x] Question source and licensing.
- [x] Copyright and naming/trademark considerations.
- [x] Final question set.
- [x] Stable `QuestionKey` scheme.
- [x] Answer scale.
- [x] Question-to-dimension mapping.
- [x] Reverse-scored items, if any.
- [x] Deterministic scoring rule.
- [x] Normalization rule, if any.
- [x] Ambiguity threshold/policy.
- [x] Deterministic final type derivation.
- [x] Result interpretation content.
- [x] Non-clinical disclaimer.

## 4. Completed: Lightweight Group Domain Review

The focused Group Domain Review is complete and recorded in `docs/domain/group-spec-aligned.md`. Confirmed:

- [x] Group lifecycle.
- [x] Membership lifecycle.
- [x] Join-request lifecycle.
- [x] Admin invariants.
- [x] Leave/disband invariants.
- [x] Group-code invitation semantics; email invitation is not required.
- [x] Sharing-consent ownership and default `NOT SHARED` behavior.

The review is complete; future changes should be driven by implementation findings or new requirements rather than repeating the full Assessment modeling exercise.

## 5. UI / UX Detailed Design and Presentation Refinement

User-visible behavior for the currently executable Identity + deterministic Assessment slice is implemented, visually refined, and validated through the existing frontend regression suite plus browser checks.

- [x] Semantic design tokens and global visual foundation.
- [x] Public + authenticated App Shells.
- [x] Small reusable `shared/ui` primitive set without introducing a general-purpose UI framework.
- [x] Landing + Register/Login presentation.
- [x] Assessment catalog/entry.
- [x] Resume vs Start New.
- [x] Questionnaire interaction.
- [x] Autosave/progress UX.
- [x] Submit confirmation and irreversible-boundary UX.
- [x] Ambiguity evidence / deterministic clarification-ready presentation.
- [ ] Provider-backed AI clarification conversation UX.
- [x] Skip Current / Decline Remaining clarification UX.
- [ ] Retry / temporary-runtime-loss UX for real AI clarification; deferred with Backend Step 8.
- [x] Exact-tie legacy direct-pole user decision UX.
- [x] Frontend F7-A dual-version Tie-break API/type compatibility (`DIRECT_POLE_SELECTION` / `CONTEXTUAL_QUESTION`, exclusive PUT union, `TIE_BREAK_QUESTION` source).
- [x] Frontend F7-B contextual Tie-break question interaction, including no-pole-mapping presentation and rolling-deploy compatibility for legacy 1.0 Sessions.
- [x] Result UX with decision provenance.
- [x] Completed History UX with pagination and canonical detail navigation.
- [x] Responsive layout, focus/hover treatment, loading/error/empty states, reduced-motion support and styled 404 presentation for the implemented surface.
- [ ] Delete historical assessment UX; deferred until Group sharing exists because deletion is cross-domain orchestration.
- [ ] Group/join/admin/sharing UX; wait for executable Group backend capability.

UI/UX answers what the user sees and does. React implementation answers how that experience is built. The current implementation checkpoint is `docs/frontend/05-implementation-checkpoint-f1-f6.md`.

## 6. Backend Detailed Design

Translate the architecture contract into implementable backend design without changing the domain semantics silently.

**Current status:** the Step 7 runtime baseline remains intact, DefinitionVersion 1.1 compatibility completed the pre-activation acceptance sweep, and the V8 activation release is deployed. New Sessions now bind 1.1 while retained 1.0 Sessions preserve their original direct-pole semantics. OpenAPI v0.5.0 matches the implemented GET/PUT boundary, version-specific projections and final-source semantics. Real provider-backed clarification interaction is the next backend milestone: Step 8.

### Domain/Application

- [x] Application use-case boundaries.
- [x] Cross-module orchestration.
- [x] Business authorization rules.
- [x] Error/failure semantics.
- [x] Retry-safe semantics for irreversible commands.
- [x] Idempotency/recovery design where appropriate.
- [x] Atomic Submit + deterministic post-processing semantics; client recovery after lost response.

### Database / Persistence

Accepted persistence artifacts are `docs/backend/04-persistence-postgresql-design.md` and `docs/backend/05-flyway-jpa-mapping.md`.

- [x] Physical PostgreSQL schema.
- [x] PK/FK/nullability decisions.
- [x] Indexes and constraints.
- [x] Durable enforcement strategy for one active Session per User + AssessmentDefinition.
- [x] Transaction/concurrency strategy.
- [x] Physical representation of questionnaire-submitted fact.
- [x] Historical-assessment deletion semantics.
- [x] Account-deletion implications (deferred feature, but data impact should be understood).
- [x] JPA mapping decisions.

### REST/API

`docs/backend/06-rest-api-contract.md` freezes REST resource/action semantics; `docs/api/openapi.yaml` now defines the exact DTO/security contract.

- [x] HTTP resource/use-case mapping.
- [x] Routes and methods.
- [x] OpenAPI v0.5.0 version-aware Tie-break GET/PUT and legacy/contextual state projections implemented in Backend; V8 activation follows the already-deployed compatibility release and promotes 1.1 after old ECS tasks have drained.
- [x] Status/error mapping.
- [x] Business authentication/authorization boundary.
- [x] Authentication transport/security mechanics (server-side Session + JDBC + cookie + CSRF).
- [x] Session-bound questionnaire read contract for exact bound DefinitionVersion.
- [x] Concrete OpenAPI DTO contract including Group display identity and clarification UNCLEAR semantics.
- [x] Spring Session JDBC Flyway ownership / schema initialization policy.
- [x] Retry/idempotency client contract where needed.

### Spring Architecture

Accepted Spring/backend architecture artifacts are `docs/backend/01-*` through `03-*` plus the persistence documents.

- [x] Logical modules -> Java/Spring package boundaries.
- [x] Dependency rules.
- [x] Application services.
- [x] Domain services only where justified.
- [x] Persistence adapters/repositories.
- [x] AI provider adapter boundary.
- [x] Testing strategy.

## 7. AI Clarification Detailed Design

- [ ] Prompt/policy design aligned with non-clinical positioning.
- [x] Domain structured-output contract (`ClarificationResult` + `AIProvenance`) used by Assessment finalization.
- [ ] Provider-specific structured-output parsing/validation.
- [ ] Temporary multi-turn runtime context design.
- [x] Sequential clarification flow: at most one `DimensionClarification` in `IN_PROGRESS` per Session.
- [ ] Decide memory/cache/temporary persistence approach without turning transcript into authoritative history.
- [x] Durable failure/retry/stale-result boundary (`FAILED_RETRYABLE` + execution correlation token); provider timeout policy wiring remains Step 8.
- [x] Provenance ownership/required fields for accepted result; provider-specific capture implementation remains later.
- [x] ClarificationPolicy provenance binding: expected/default revision in DefinitionVersion + actual accepted-run provenance.
- [ ] Safety/privacy review.

Exact mid-conversation resume after runtime loss remains out of MVP scope.

## 8. Frontend Implementation

**Current status:** Frontend **F1-F6, post-F6 UI optimization, DefinitionVersion 1.1 compatibility through F7-B, F7-C Result interpretation/content, and F7-D Landing hero integration are complete**. The browser supports both retained 1.0 and 1.1 Tie-break semantics, completed results include versioned presentation content plus one optimized illustration per type, and the public Landing page uses the optimized four-profile hero artwork. The F7-A–F7-E compatibility/presentation batch is review-complete; the separate activation release requires no additional Frontend code.

- [x] F0 — React + TypeScript architecture/integration baseline.
- [x] F1 — React + TypeScript/Vite foundation and test infrastructure.
- [x] F2 — Register / Login / Session restore / protected routes / CSRF recovery / Logout.
- [x] F3 — Assessment catalog/detail + Start/Resume/Restart + canonical Session routing.
- [x] F4 — Questionnaire rendering, local draft ownership, debounced autosave, Submit and recovery.
- [x] F5 — deterministic Clarification read/Skip/Skip Remaining + exact Tie-break/finalization.
- [x] F6 — Result read model + completed History + canonical detail/navigation integration.
- [x] F7-A — dual-version Tie-break TypeScript/API contract and `TIE_BREAK_QUESTION` source support.
- [x] F7-B — contextual Tie-break GET/render/confirm/PUT flow; 1.0 intentionally avoids the new GET dependency during parallel rollout.
- [x] F7-C — 8 preference-letter descriptions + 16 personality-type descriptions + strengths/blind spots + optimized result imagery, while preserving evidence and Decision trace.
- [x] F7-D — Landing hero visual integration with optimized WebP, responsive layout, accessible image text, intrinsic sizing and LCP-oriented loading metadata.
- [x] F7-E — final frontend regression/documentation/release review; compatibility release ready, activation intentionally separated.
- [x] Post-F6 UI optimization — design tokens, App Shells, reusable UI primitives, public/auth/dashboard/workflow redesign, responsive/error/empty/404 polish.
- [x] Loading/error/retry UX for implemented Identity + deterministic Assessment flows.
- [x] Vitest / React Testing Library / MSW coverage plus repeated real-browser integration verification.
- [ ] Dedicated Playwright E2E automation; defer until a repeatable CI/full-stack environment exists.
- [ ] Provider-backed clarification Start/Continue/Retry UI; defer with Backend Step 8.
- [ ] Historical Assessment deletion UI; defer until Group sharing + deletion orchestration exist.
- [ ] Group/join/admin/sharing UI; wait for executable Group backend capability.

Accepted F0 baseline and the F1-F6 implementation checkpoint live under `docs/frontend/`. Frontend continues to express user intent and render backend-authoritative lifecycle state rather than becoming a second business state machine.

## 9. Backend Implementation

Current backend checkpoint: **Assessment Step 7 complete + DefinitionVersion 1.1 activation release**.

- [x] Java 21 + Spring Boot implementation foundation.
- [x] PostgreSQL persistence foundation with Flyway + JPA/Hibernate.
- [x] Authentication: Register / Login / Session / CSRF / `/me` / Logout.
- [x] Assessment catalog, Start/Resume, Session-bound questionnaire read and autosave.
- [x] deterministic Submit, scoring, ambiguity evaluation and immediate finalization.
- [x] Restart / Start New with targeted locking and retry/recovery semantics.
- [x] completed Assessment History + Historical Detail read side.
- [x] Clarification + Tie-break mutation workflow (Step 7).
- [x] DefinitionVersion 1.1 Specification Domain + persistence expansion.
- [x] Version-aware Tie-break GET interaction + exclusive legacy/contextual PUT + contextual Session projection (Step 2C).
- [x] Provenance-aware finalization retaining `USER_TIE_BREAK` for 1.0 and producing `TIE_BREAK_QUESTION` for contextual facts (Step 2D).
- [x] DefinitionVersion 1.1 pre-activation acceptance sweep (Step 2E): immutable content equivalence, UNCLEAR/SKIPPED matrix, retries/conflicts, 50/50 evidence, persisted history and Session-lock finalization race.
- [x] DefinitionVersion 1.1 activation migration + post-activation retained-1.0/new-1.1 binding verification (V8 / checkpoint 17).
- [x] Frontend contextual Tie-break integration — F7-A contract compatibility + F7-B UI interaction complete; activation requires no additional Frontend code.
- [ ] real AI integration adapter / runtime context (Step 8).
- [ ] Historical assessment deletion orchestration.
- [ ] Group/membership/sharing.
- [x] Unit/Application/Web MVC/PostgreSQL integration test foundation and Step 1-7 coverage.
- [x] Restart concurrency/retry/recovery tests for implemented critical invariants.
- [x] Clarification stale-result/retry/restart recovery and PostgreSQL one-`IN_PROGRESS` constraint coverage.
- [ ] Dedicated simultaneous-transaction Clarification/finalization race tests.
- [ ] Group/sharing/deletion concurrency tests.

Implementation sequencing note:

```text
AWS / Container / CI-CD foundation (completed)
        ↓
DefinitionVersion 1.1 activation + automatic CD verification (completed)
        ↓
Provider-backed LLM clarification (Step 8)
        ↓
Group backend / sharing
        ↓
Historical Assessment deletion orchestration + frontend deletion UX
```

Provider-backed LLM Step 8 was intentionally postponed until the Cloud/CI-CD foundation existed; that infrastructure milestone is now complete enough for product work to resume.

Historical deletion remains in Core MVP, but implementing it before Group sharing would create a fake cross-module dependency or require rework.

## 10. Containerization and Local Environment

**Status: production containerization is implemented and deployed.**

- [x] Multi-stage Dockerfile for the Spring Boot backend.
- [x] Non-root runtime user.
- [x] Exposed runtime port verified in CI.
- [x] Linux/amd64 image build used by deployment.
- [x] Local PostgreSQL environment.
- [x] Documented local Backend + Frontend startup through Maven/Vite with relative `/api` proxying.
- [x] Production Spring profile and environment-variable configuration.
- [ ] Optional full production-like local orchestration for all components; not required for the current AWS milestone.

## 11. AWS / Infrastructure as Code

**Status: first full-stack AWS deployment complete.**

- [x] AWS target architecture implemented in `ap-northeast-1`.
- [x] VPC, two public app subnets and two private DB subnets.
- [x] Security Group boundaries for CloudFront/ALB/ECS/RDS.
- [x] RDS PostgreSQL deployed privately.
- [x] ECR backend repository with immutable tags.
- [x] ALB + ECS Fargate Spring Boot service.
- [x] Private S3 frontend bucket + CloudFront OAC.
- [x] CloudFront same-origin routing for frontend and `/api/*`.
- [x] CloudWatch backend logging.
- [x] Secrets Manager-backed RDS credentials.
- [x] Terraform remote state in S3 with native lockfile support.
- [x] Cost-aware no-NAT MVP topology.
- [x] Deployment validation through browser and public smoke tests.
- [x] ECS rollout tuning based on measured Spring Boot startup.
- [ ] Custom domain / ACM.
- [ ] Optional WAF/origin hardening.
- [ ] Optional private ECS + NAT/VPC endpoints.
- [ ] Higher-availability production profile (multi-task ECS, RDS Multi-AZ).

Detailed as-built documentation: `docs/deployment/aws-deployment.md`.

## 12. CI/CD

**Status: CI + OIDC + automatic post-CI CD verified end-to-end.**

- [x] GitHub Actions CI.
- [x] Backend Maven `verify`.
- [x] Frontend lint/test/build.
- [x] Production-bundle localhost guard.
- [x] Backend Docker build/runtime checks.
- [x] GitHub OIDC authentication to AWS.
- [x] Immutable repository/branch trust restriction.
- [x] Least-privilege AWS deploy role.
- [x] Backend image push to ECR.
- [x] ECS Task Definition revision + Service update.
- [x] Frontend S3 sync + CloudFront invalidation.
- [x] Public post-deployment smoke test.
- [x] Manual production CD verified end-to-end.
- [x] Automatic CD trigger implemented behind `CD_ENABLED`.
- [x] Successful `main` CI → automatic CD verified with a real feature/activation release.

Detailed pipeline documentation: `docs/deployment/ci-cd.md`.

## 13. Quality, Security, and Observability

Current baseline:

- [x] Server-side Session + CSRF security.
- [x] Non-root backend container.
- [x] Private RDS.
- [x] Private S3 + CloudFront OAC.
- [x] Secrets Manager for production DB credentials.
- [x] GitHub OIDC instead of long-lived AWS deployment credentials.
- [x] Least-privilege deployment role.
- [x] CloudWatch backend logs.
- [x] ALB health checks + ECS rolling deployment.
- [ ] CloudWatch alarms/dashboard.
- [ ] Automated rollback/circuit-breaker policy.
- [ ] Browser E2E deployment test.
- [ ] WAF/custom origin-header hardening if justified.
- [ ] Formal LLM privacy/safety review when provider-backed clarification is implemented.

## 14. Portfolio Finalization

- [x] GitHub README shows the current production architecture.
- [x] Architecture document contains the production architecture diagram first.
- [x] CI/CD flow diagram follows the architecture diagram.
- [x] Local-run instructions.
- [x] AWS deployment explanation.
- [x] CI/CD and OIDC explanation.
- [x] Testing strategy explanation.
- [x] Key ADR/index.
- [x] Future-work/options document.
- [x] Final automatic-CD live verification.
- [ ] Final MVP feature-completion review after LLM / Group / deletion scope.
- [ ] Final interview-ready project retrospective after MVP completion.

## 15. Deferred / Post-MVP

- Password reset if not completed during MVP.
- Big Five and additional assessment types.
- Email-based Group invitations.
- Exact mid-AI-conversation resume across runtime loss.
- Full AI transcript/execution audit history.
- `ClarificationAttempt[]` audit model.
- Advanced group administration/ownership transfer unless required by MVP behavior.
- Delete Account UI/API unless reprioritized; data semantics should still be understood during database design.
- Advanced analytics/reporting.
