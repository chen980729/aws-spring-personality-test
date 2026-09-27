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

## 5. UI / UX Detailed Design

Define user-visible behavior before or alongside Frontend implementation:

- [ ] Assessment catalog/entry.
- [ ] Resume vs Start New.
- [ ] Questionnaire interaction.
- [ ] Autosave/progress UX.
- [ ] Submit confirmation and irreversible-boundary UX.
- [ ] Ambiguity explanation/choice.
- [ ] AI clarification conversation UX.
- [ ] Retry / Skip Current / Decline Remaining AI UX.
- [ ] Recovery UX when temporary AI conversation context is lost.
- [ ] Result UX.
- [ ] History UX.
- [ ] Delete historical assessment UX.
- [ ] Group/join/admin/sharing UX.

UI/UX answers what the user sees and does. React implementation answers how that experience is built.

## 6. Backend Detailed Design

Translate the architecture contract into implementable backend design without changing the domain semantics silently.

**Current status:** design remains accepted and implementation is aligned through **Assessment Step 6**. OpenAPI v0.3.0 is synchronized with the implemented History contract. Step 7 Clarification/Tie-break mutation is next.

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
- [x] Exact DTO schema / OpenAPI v0.3.0 synchronized through Assessment Step 6.
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
- [ ] Structured output contract used by Assessment.
- [ ] Temporary multi-turn runtime context design.
- [x] Sequential clarification flow: at most one `DimensionClarification` in `IN_PROGRESS` per Session.
- [ ] Decide memory/cache/temporary persistence approach without turning transcript into authoritative history.
- [ ] Failure/timeout/retry behavior.
- [x] Provenance ownership/required fields for accepted result; provider-specific capture implementation remains later.
- [x] ClarificationPolicy provenance binding: expected/default revision in DefinitionVersion + actual accepted-run provenance.
- [ ] Safety/privacy review.

Exact mid-conversation resume after runtime loss remains out of MVP scope.

## 8. Frontend Implementation

- [ ] React + TypeScript project architecture.
- [ ] Authentication experience.
- [ ] Assessment catalog/entry.
- [ ] Questionnaire/resume/autosave.
- [ ] Clarification flow.
- [ ] Result/history/deletion.
- [ ] Group/join/admin/sharing.
- [ ] Loading/error/retry UX.
- [ ] Frontend tests.

Frontend must express user intent; it must not become the source of truth for backend lifecycle or authorization rules.

## 9. Backend Implementation

Current backend checkpoint: **Assessment Step 6 complete**.

- [x] Java 21 + Spring Boot implementation foundation.
- [x] PostgreSQL persistence foundation with Flyway + JPA/Hibernate.
- [x] Authentication: Register / Login / Session / CSRF / `/me` / Logout.
- [x] Assessment catalog, Start/Resume, Session-bound questionnaire read and autosave.
- [x] deterministic Submit, scoring, ambiguity evaluation and immediate finalization.
- [x] Restart / Start New with targeted locking and retry/recovery semantics.
- [x] completed Assessment History + Historical Detail read side.
- [ ] Clarification + Tie-break mutation workflow (Step 7).
- [ ] real AI integration adapter / runtime context (Step 8 or implementation slice following Step 7 boundaries).
- [ ] Historical assessment deletion orchestration.
- [ ] Group/membership/sharing.
- [x] Unit/Application/Web MVC/PostgreSQL integration test foundation and Step 1-6 coverage.
- [x] Restart concurrency/retry/recovery tests for implemented critical invariants.
- [ ] Clarification/finalization concurrency tests.
- [ ] Group/sharing/deletion concurrency tests.

## 10. Containerization and Local Environment

- [ ] Dockerfile(s).
- [ ] Local PostgreSQL environment.
- [ ] Reproducible local startup.
- [ ] Environment/configuration strategy.

## 11. AWS / Infrastructure as Code

- [ ] AWS target architecture detailed design.
- [ ] Networking/security boundaries.
- [ ] Managed PostgreSQL decision.
- [ ] Container/application hosting decision.
- [ ] Secrets/configuration management.
- [ ] Terraform implementation.
- [ ] Deployment validation.
- [ ] Cost-awareness review for a personal portfolio project.

## 12. CI/CD

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
