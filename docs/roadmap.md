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
- [x] Exact-tie user decision UX.
- [x] Result UX with decision provenance.
- [x] Completed History UX with pagination and canonical detail navigation.
- [x] Responsive layout, focus/hover treatment, loading/error/empty states, reduced-motion support and styled 404 presentation for the implemented surface.
- [ ] Delete historical assessment UX; deferred until Group sharing exists because deletion is cross-domain orchestration.
- [ ] Group/join/admin/sharing UX; wait for executable Group backend capability.

UI/UX answers what the user sees and does. React implementation answers how that experience is built. The current implementation checkpoint is `docs/frontend/05-implementation-checkpoint-f1-f6.md`.

## 6. Backend Detailed Design

Translate the architecture contract into implementable backend design without changing the domain semantics silently.

**Current status:** design remains accepted and implementation is aligned through **Assessment Step 7**. OpenAPI v0.4.0 reflects the deterministic Clarification/Tie-break HTTP alignment; real provider-backed clarification interaction remains Step 8.

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
- [x] Design-first DTO schema / OpenAPI v0.4.0 aligned through the deterministic Step 7 HTTP boundary.
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

**Current status:** Frontend **F1-F6 plus the post-F6 UI optimization checkpoint are complete for the executable Authentication + deterministic Assessment slice**. The browser application covers Session/CSRF authentication, Assessment entry, questionnaire/autosave/submit, deterministic clarification Skip/Tie-break, Result, completed History and canonical navigation, with a consistent responsive presentation layer. Frontend feature/UI work is intentionally paused before deletion/Group/real-LLM work while the project moves to AWS/CI/CD.

- [x] F0 — React + TypeScript architecture/integration baseline.
- [x] F1 — React + TypeScript/Vite foundation and test infrastructure.
- [x] F2 — Register / Login / Session restore / protected routes / CSRF recovery / Logout.
- [x] F3 — Assessment catalog/detail + Start/Resume/Restart + canonical Session routing.
- [x] F4 — Questionnaire rendering, local draft ownership, debounced autosave, Submit and recovery.
- [x] F5 — deterministic Clarification read/Skip/Skip Remaining + exact Tie-break/finalization.
- [x] F6 — Result read model + completed History + canonical detail/navigation integration.
- [x] Post-F6 UI optimization — design tokens, App Shells, reusable UI primitives, public/auth/dashboard/workflow redesign, responsive/error/empty/404 polish.
- [x] Loading/error/retry UX for implemented Identity + deterministic Assessment flows.
- [x] Vitest / React Testing Library / MSW coverage plus repeated real-browser integration verification.
- [ ] Dedicated Playwright E2E automation; defer until a repeatable CI/full-stack environment exists.
- [ ] Provider-backed clarification Start/Continue/Retry UI; defer with Backend Step 8.
- [ ] Historical Assessment deletion UI; defer until Group sharing + deletion orchestration exist.
- [ ] Group/join/admin/sharing UI; wait for executable Group backend capability.

Accepted F0 baseline and the F1-F6 implementation checkpoint live under `docs/frontend/`. Frontend continues to express user intent and render backend-authoritative lifecycle state rather than becoming a second business state machine.

## 9. Backend Implementation

Current backend checkpoint: **Assessment Step 7 complete**.

- [x] Java 21 + Spring Boot implementation foundation.
- [x] PostgreSQL persistence foundation with Flyway + JPA/Hibernate.
- [x] Authentication: Register / Login / Session / CSRF / `/me` / Logout.
- [x] Assessment catalog, Start/Resume, Session-bound questionnaire read and autosave.
- [x] deterministic Submit, scoring, ambiguity evaluation and immediate finalization.
- [x] Restart / Start New with targeted locking and retry/recovery semantics.
- [x] completed Assessment History + Historical Detail read side.
- [x] Clarification + Tie-break mutation workflow (Step 7).
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
Cloud / Container / CI-CD work (current)
        ↓
Group backend + sharing boundary (later)
        ↓
Historical Assessment deletion orchestration + frontend deletion UX

Provider-backed LLM Step 8 is intentionally postponed until after the Cloud/CI-CD line is established.
```

Historical deletion remains in Core MVP, but implementing it before Group sharing would create a fake cross-module dependency or require rework.

## 10. Containerization and Local Environment

**Current active project focus begins here.** The application is locally runnable as Spring Boot + Vite + PostgreSQL; production packaging is the next delivery boundary.

- [ ] Dockerfile(s) for deployable application components.
- [x] Local PostgreSQL environment.
- [x] Documented local Backend + Frontend startup through Maven/Vite with relative `/api` proxying.
- [ ] Reproducible production-like local startup for the complete application.
- [ ] Production environment/configuration strategy.

## 11. AWS / Infrastructure as Code

**Current active focus:** resume AWS deployment work after the deterministic full-stack Assessment slice reached Frontend F6 and completed its current-scope UI optimization.

- [ ] AWS target architecture detailed design.
- [ ] Networking/security boundaries.
- [ ] Managed PostgreSQL decision.
- [ ] Container/application hosting decision.
- [ ] Secrets/configuration management.
- [ ] Terraform implementation.
- [ ] Deployment validation.
- [ ] Cost-awareness review for a personal portfolio project.

## 12. CI/CD

**Current active focus together with AWS deployment.** CI should verify both already-established Backend and Frontend quality gates before deployment automation is added.

- [ ] GitHub Actions CI.
- [ ] Backend tests/build.
- [ ] Frontend tests/build.
- [ ] Container build.
- [ ] Infrastructure/deployment workflow.
- [ ] Safe environment/secret handling.

## 13. Quality, Security, and Observability

- [ ] Authorization/security review.
- [ ] Sensitive-data and AI data-minimization review.
- [ ] Logs/metrics appropriate for debugging without leaking personal content.
- [ ] Failure/recovery testing.
- [ ] Basic performance review.

## 14. Portfolio Finalization

- [ ] Final README.
- [ ] Architecture diagram.
- [ ] Local-run instructions.
- [ ] AWS deployment explanation.
- [ ] CI/CD explanation.
- [ ] Testing strategy explanation.
- [ ] Key ADR/index.
- [ ] Interview-ready explanation of major technical decisions and trade-offs.

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
