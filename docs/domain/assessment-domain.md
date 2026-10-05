# Assessment Domain Model

> **DefinitionVersion 1.1 status (ADR-0018):** Backend Domain/Application/Persistence compatibility is implemented and acceptance-tested, and Frontend dual-version integration is complete through F7-D. 1.1 remains DRAFT; activation stays separate until the compatibility release is deployed and old ECS tasks have drained. Published 1.0 remains immutable and supported.

> **Status:** Accepted Assessment Domain Baseline — Step 7 legacy behavior plus implemented dual-version tie-break semantics
> **Last updated:** 2026-10-06

## 1. Scope

This document records the accepted Assessment domain baseline. It contains final domain conclusions and intentionally avoids framework/persistence implementation details.

## 2. Ubiquitous Language

- **AssessmentDefinition** — long-lived identity of an assessment type, e.g. `SIXTEEN_PERSONALITY`.
- **AssessmentDefinitionVersion** — one immutable executable assessment specification.
- **AssessmentSession** — one user's complete attempt against one exact definition version.
- **QuestionnaireResponse** — immutable value representing the session's current answer snapshot.
- **QuestionnaireSubmitted** — Domain Event representing submission occurrence.
- **InitialAssessmentResult** — immutable deterministic scoring evidence.
- **DimensionClarification** — one logical clarification lifecycle for one ambiguous dimension.
- **ClarificationResult** — accepted immutable clarification conclusion.
- **AIProvenance** — effective successful AI execution provenance for the accepted clarification result.
- **DimensionTieBreak** — persisted explicit user decision resolving an exact-tie dimension after `UNCLEAR` / `SKIPPED`: direct-pole selection in legacy 1.0 or contextual option selection in 1.1.
- **FinalAssessmentResult** — immutable final assessment outcome.

## 3. AssessmentSession Lifecycle

Top-level Session lifecycle states:

```text
IN_PROGRESS
AWAITING_CLARIFICATION
CLARIFICATION_IN_PROGRESS
COMPLETED
ABANDONED
```

Semantics:

- `IN_PROGRESS` — active pre-submission questionnaire state. Questionnaire answering is active and response replacement is allowed. In MVP, successful submission and deterministic post-submission processing are atomic, so a committed Session does not remain `IN_PROGRESS` after submission.
- `AWAITING_CLARIFICATION` — deterministic scoring found one or more ambiguous dimensions and no clarification conversation is currently active.
- `CLARIFICATION_IN_PROGRESS` — the session is in the active AI-assisted clarification phase for one eligible dimension. MVP allows at most one `DimensionClarification` to be `IN_PROGRESS` in a Session at a time.
- `COMPLETED` — terminal state; one final result has been accepted and assessment business content is immutable.
- `ABANDONED` — terminal state; the attempt is no longer active. It may be entered from any non-terminal active Session state: `IN_PROGRESS`, `AWAITING_CLARIFICATION`, or `CLARIFICATION_IN_PROGRESS`. `COMPLETED` and `ABANDONED` cannot transition to `ABANDONED`.

`Resume` is **not** a lifecycle transition. Resume loads the existing persisted session and continues from its valid business state.

`Restart / Start New` is also not a new lifecycle status. It is a cross-Session business command: one identified active Session transitions to `ABANDONED`, while a distinct replacement `AssessmentSession` begins in `IN_PROGRESS`. The old Session keeps its original DefinitionVersion binding and historical evidence. The replacement binds the same `AssessmentDefinition` but the unique current `AVAILABLE` `AssessmentDefinitionVersion`.

A completed historical Session is not “restarted”. `COMPLETED` remains immutable/terminal; taking the Assessment again after completion creates a separate new Session through normal Start semantics when no active Session exists.

The following are **not** top-level Session lifecycle states:

- `QUESTIONNAIRE_SUBMITTED` — submission event/durable fact/business boundary.
- `INITIAL_SCORED` — important result/fact, not a top-level user-facing lifecycle state.
- `FINALIZING` — processing concern unless future asynchronous semantics require stronger business meaning.

### Clarification runtime recovery

`CLARIFICATION_IN_PROGRESS` does not make an incomplete AI conversation authoritative history. Conversation context is ephemeral in MVP.

If runtime conversation context is lost before an accepted `ClarificationResult` is durably persisted, recovery may normalize the affected clarification to a clarification-ready/retryable condition, and the Session can return to `AWAITING_CLARIFICATION`. The existing submitted response and `InitialAssessmentResult` remain unchanged.

If the parent Session is transitioned to `ABANDONED` while an external LLM call is in flight, the late provider result is stale. The post-call transaction must reload/revalidate the Session and discard that result rather than mutate the abandoned attempt.

## 4. DimensionClarification Local Lifecycle

Conceptual local states:

```text
PENDING
IN_PROGRESS
CLARIFIED
SKIPPED
FAILED_RETRYABLE
```

- `PENDING` — eligible ambiguous dimension has not produced an accepted clarification result and has not been skipped.
- `IN_PROGRESS` — temporary clarification interaction is active.
- `CLARIFIED` — a valid accepted `ClarificationResult` is persisted. The accepted result may be `RESOLVED` with a selected pole or `UNCLEAR` with no selected pole; `CLARIFIED` means the clarification interaction completed successfully at the business level, not that AI necessarily chose a pole.
- `SKIPPED` — user chose not to use clarification for that dimension.
- `FAILED_RETRYABLE` — a known technical failure allows retry without treating the assessment as terminally failed. Explicit Retry reuses this same logical clarification lifecycle and, after revalidation, may transition it back to `IN_PROGRESS`; Skip remains available as the alternative user action.

One logical `DimensionClarification` exists per `Session + Dimension`; technical retries do not create a second logical clarification lifecycle.

For MVP, clarification execution is sequential at the Session level: at most one `DimensionClarification` may be `IN_PROGRESS` at a time. Parallel clarification conversations within the same Session are out of scope.

## 5. QuestionnaireSubmitted: Event, Durable Fact, and Boundary

These are related but distinct concepts.

### 5.1 Domain Event

`QuestionnaireSubmitted` means the business-significant submission action occurred.

### 5.2 Durable Domain Fact

The system must durably know:

> The questionnaire has been submitted.

Consequences:

- The response is frozen for this attempt.
- Crash/restart must not make the response editable again after a successful Submit commit.
- Submit plus deterministic scoring, ambiguity evaluation, clarification setup, and any immediate finalization is atomic in MVP; a committed `submitted-but-not-scored` state is not part of the normal lifecycle.

### 5.3 Business Boundary

Submit crosses:

```text
mutable assessment-answering phase
        ->
irreversible submitted phase
```

The project is not Event Sourcing. A Domain Event does not imply a requirement to persist a complete event log.

## 6. Entity and Value Object Classification

### Entities

- `AssessmentDefinition`
- `AssessmentDefinitionVersion`
- `AssessmentSession`
- `DimensionClarification`

### Value Objects

- `QuestionnaireDefinition`
- `QuestionDefinition`
- `TieBreakQuestionDefinition` (accepted 1.1 specification addition)
- `DimensionDefinition`
- `ScoringPolicy`
- `AmbiguityPolicy`
- `FinalizationPolicy`
- `QuestionnaireResponse`
- `Answer`
- `InitialAssessmentResult`
- `InitialDimensionResult`
- `ClarificationResult`
- `AIProvenance`
- `DimensionTieBreak`
- `FinalAssessmentResult`
- `FinalDimensionConclusion`

### Domain Event

- `QuestionnaireSubmitted`

DDD Entity identity is business continuity, not JPA mapping. A Value Object can contain a stable key/code. Immutability does not automatically imply Value Object, and mutability does not automatically imply Entity.

## 7. Aggregate Design

### 7.1 AssessmentSession Aggregate

```text
AssessmentSession (Entity / Aggregate Root)
├── QuestionnaireResponse (VO)
│   └── Answer[] (VO)
├── InitialAssessmentResult (VO)
│   └── InitialDimensionResult[] (VO)
└── FinalAssessmentResult (VO)
    └── FinalDimensionConclusion[] (VO)
```

`AssessmentSession` owns the attempt lifecycle, immutable submitted evidence, Session status, and final result. It is also the concurrency anchor for clarification/finalization commands, but it does not contain the persisted Clarification collection in its in-memory Aggregate.

### 7.2 DimensionClarification Aggregate

```text
DimensionClarification (Entity / Aggregate Root)
├── AssessmentSessionId (reference)
├── DimensionCode
├── lifecycle status
├── activeExecutionToken (opaque VO, only while IN_PROGRESS)
├── ClarificationResult (VO, when accepted)
└── AIProvenance (VO, when accepted)
```

One logical `DimensionClarification` exists for one `AssessmentSession + Dimension`. It is a separate Assessment Aggregate because it has its own durable lifecycle and must cross an external LLM boundary without holding the Session transaction open. Session-wide eligibility/finalization rules are coordinated by Assessment Application using `AssessmentSession` as the concurrency/revalidation anchor plus durable uniqueness constraints. See ADR-0016 for the implementation-facing consistency decision.

### 7.3 AssessmentDefinitionVersion Aggregate

```text
AssessmentDefinitionVersion (Entity / Aggregate Root)
├── QuestionnaireDefinition (VO)
│   └── QuestionDefinition[] (VO)
├── DimensionDefinition[] (VO)
├── TieBreakQuestionDefinition[] (VO, 1.1 only)
├── ScoringPolicy (VO)
├── AmbiguityPolicy (VO)
└── FinalizationPolicy (VO)
```

Once executable/available to users, this specification is immutable.

### 7.4 AssessmentDefinition Aggregate

`AssessmentDefinition` remains a small provisional Aggregate Root representing the long-lived assessment identity and potential future version-governance boundary. It must not receive artificial behavior merely to justify Aggregate status.

### 7.5 Accepted 1.1 specification and tie-break fact

Conceptually, `AssessmentSpecification` adds:

```text
TieBreakQuestionDefinition[]
  questionId
  dimension
  instruction
  prompt
  options[2]
    optionId
    text
    resolvedPole
```

Version-aware loading/validation is required for backward compatibility:

- `FinalizationPolicy` v1 (published 1.0) may have no `tieBreakQuestions` field in persisted JSONB, or an empty collection if represented by newer code. That absence/emptiness means legacy direct-pole semantics and must remain loadable.
- `FinalizationPolicy` v2 (1.1) requires `TieBreakQuestionDefinition[]` and exactly one valid question for every dimension.
- A v2 question must reference an existing `DimensionDefinition`.
- Question IDs are unique across the specification.
- Each v2 question contains exactly two options.
- Option IDs are unique within that question.
- The two options resolve to the two different poles of that dimension.
- Every `resolvedPole` belongs to the referenced `DimensionDefinition`.

The persistence mapper must therefore not globally require `tieBreakQuestions` for every historical specification. Requirement is selected by the specification's finalization semantics, not by the presence of newer application code.

`resolvedPole` is Backend-owned specification data, never a field in a public contextual option DTO. Resolving `selectedOptionId -> PoleCode` always uses the Session's bound immutable DefinitionVersion and the question for the requested dimension, never display position or the currently AVAILABLE version.

Preserve the existing `DimensionTieBreak` VO; its conceptual evolution is:

```text
sessionId
dimension
questionId        nullable for legacy 1.0; required for 1.1
selectedOptionId  nullable for legacy 1.0; required for 1.1
resolvedPole
decidedAt
```

Legacy 1.0 facts retain the directly selected pole as their resolved outcome and contain no fabricated question/option IDs. `TieBreakResponse` is not a new Aggregate or Value Object. For 1.1, validate the submitted question ID against the bound dimension's question and the option ID within that question before deriving `resolvedPole`; a client-supplied pole is not contextual evidence. The fact retains the original `decidedAt` on equivalent retries. A conflicting second selection cannot rewrite accepted history.

Both paths require `rawScore == 0` with clarification `UNCLEAR` / `SKIPPED`; neither changes the raw score or `50 / 50` evidence. A valid `RESOLVED` clarification already supplies the preference. Technical failure alone never qualifies as `UNCLEAR` or Skip.

## 8. Assessment Workflow Invariants

### AS-INV-01 Stable Session Context

Session owner and bound `AssessmentDefinitionVersion` never change.

### AS-INV-02 Response Replacement Boundary

Before questionnaire submit, the Session may replace its `QuestionnaireResponse` with a new immutable value. After submit, it may not.

### AS-INV-03 Initial Result Creation

`InitialAssessmentResult` can be created only after questionnaire submission and at most once.

### AS-INV-04 Initial Evidence Preservation

`InitialAssessmentResult` cannot be replaced or modified. AI clarification cannot rewrite it.

### AS-INV-05 Clarification Eligibility

Only dimensions already classified as ambiguous in the parent Session's `InitialAssessmentResult` are eligible for clarification. Because `DimensionClarification` is a separate Aggregate, the Assessment Application revalidates this rule against the locked Session before mutation.

### AS-INV-06 One Clarification Lifecycle per Dimension

At most one logical `DimensionClarification` exists per `Session + Dimension`. Retries remain within that Aggregate lifecycle. Durable uniqueness is enforced by persistence as well as in normal Application validation.

### AS-INV-07 Single Active Clarification (MVP)

At most one `DimensionClarification` may be `IN_PROGRESS` for one `AssessmentSession` at a time. A different eligible dimension may start only when no other Clarification Aggregate is actively `IN_PROGRESS`. Application coordination uses the Session concurrency anchor and durable conditional uniqueness; parallel clarification conversations are not supported in MVP.

### AS-INV-08 Clarification State Consistency

- `CLARIFIED` requires a valid accepted `ClarificationResult` (`RESOLVED` with a pole, or `UNCLEAR` with no pole).
- `IN_PROGRESS` requires exactly one current opaque execution token; every non-`IN_PROGRESS` state has no active token.
- Retry keeps the same logical Clarification Aggregate but replaces the active execution token. Accept, technical failure and Skip clear it.
- `SKIPPED` must not imply an AI judgment was produced.
- Local state, active execution identity and accepted outcome must not contradict each other.

### AS-INV-09 Completion Readiness

All ambiguous dimensions must have corresponding Clarification Aggregates in terminal workflow states `CLARIFIED` or `SKIPPED` before completion. This condition is necessary but not always sufficient.

If an ambiguous dimension was an exact questionnaire tie (`questionnairePreference == null`) and its accepted clarification result is `UNCLEAR`, or that clarification is `SKIPPED`, the dimension still has no final preference. In that case an explicit persisted `DimensionTieBreak` is required before completion. A resolved clarification that selects a valid pole already supplies the final preference and does not require a tie-break.

Finalization may proceed only when every dimension can produce exactly one `FinalDimensionConclusion`: either directly from non-ambiguous questionnaire evidence, from an accepted clarification/fallback path, or from a valid explicit user tie-break where required. Finalization evaluates these cross-Aggregate facts under the locked Session transaction.

### AS-INV-10 Completion Consistency

A legal completion transition produces exactly one `FinalAssessmentResult` and enters `COMPLETED`. When a final dimension conclusion comes from an explicit exact-tie decision, its decision source is `USER_TIE_BREAK` for legacy 1.0 direct-pole decisions or `TIE_BREAK_QUESTION` for 1.1 contextual-question decisions; the tie-break fact remains persisted separately from the derived final result. Completed assessment business content cannot be edited in place.

Deletion of the entire historical assessment record is a separate owner capability and does not contradict immutability.

### AS-INV-11 Abandonment

`IN_PROGRESS`, `AWAITING_CLARIFICATION`, and `CLARIFICATION_IN_PROGRESS` may transition to `ABANDONED`. `COMPLETED` and `ABANDONED` are terminal and cannot be abandoned again. After parent abandonment, no late clarification result may be accepted into the Session.

## 9. Cross-Aggregate / Domain Invariants

### DOMAIN-INV-01 One Active Session

For a given `User + AssessmentDefinition`, at most one active `AssessmentSession` may exist.

This invariant must be enforceable from durable system state and survive application restart. Frontend pre-checks are insufficient because concurrent requests can race.

### DOMAIN-INV-02 Start New Coordination

Normal `StartAssessment` resumes an existing active Session. Explicit `Start New` is a separate business intent that targets the exact active Session being replaced.

The command must:

- revalidate ownership and active state for that identified old Session under concurrency;
- atomically transition the old Session to `ABANDONED`;
- create exactly one replacement Session for the same `AssessmentDefinition`;
- bind the replacement to the Definition's unique current `AVAILABLE` `AssessmentDefinitionVersion`;
- leave the old Session bound to its original version and preserve its historical facts;
- preserve `DOMAIN-INV-01` throughout the committed result.

A retry against the already-abandoned old Session must not abandon or replace the successor Session. Concurrent Restart requests against the same old `sessionId` may produce at most one successful replacement.

### DOMAIN-INV-03 Definition / Version Consistency

A Session's bound DefinitionVersion must belong to the intended AssessmentDefinition.

### DOMAIN-INV-04 Questionnaire Validation Against Bound Specification

Before submit, the response must satisfy the QuestionnaireDefinition of the bound DefinitionVersion.

### DOMAIN-INV-05 Bound Specification Consistency

All deterministic computation and decision-making for a Session must use the exact specification of its bound DefinitionVersion. Rules from another version must not be mixed into the attempt.

### DOMAIN-INV-06 One AVAILABLE Version per Definition (MVP)

For one `AssessmentDefinition`, at most one `AssessmentDefinitionVersion` may be `AVAILABLE` at a time. `StartAssessment(assessmentCode)` therefore resolves one unique executable version without requiring the client to select a version.

### DOMAIN-INV-07 Session / Clarification Coordination

Clarification mutations that can affect Session status or finalization must coordinate the `AssessmentSession` and target `DimensionClarification` Aggregate within one short consistency boundary, using the Session as the concurrency/revalidation anchor. No consistency lock is held across an external LLM call.

Starting/retrying external work creates a new opaque active execution token on the Clarification. A later completion may be accepted only after both Aggregates are reloaded/revalidated **and** its execution token still matches the Clarification's current active token. This prevents a late result from an earlier failed execution from being accepted after the same Clarification has already re-entered `IN_PROGRESS` on retry. Concrete row-lock/constraint mechanics live in Backend Detailed Design.

## 10. Versioning and Governance Rules

### VER-01 Semantic Change Requires a New Version

Any semantic change to questionnaire content, dimensions, scoring, ambiguity rules, or finalization behavior requires a new `AssessmentDefinitionVersion` once the existing version is executable/available to users.

### VER-02 Technical Refactor Does Not Require a New Domain Version

Implementation refactors, performance improvements, or SQL changes that preserve assessment semantics do not create a new assessment version.

### VER-03 Accepted 1.0 to 1.1 transition

`SIXTEEN_PERSONALITY` 1.1 reuses exactly the same 48 questionnaire questions as 1.0. `ScoringPolicy`, `AmbiguityPolicy`, and clarification behavior (including the expected/default clarification policy revision) remain unchanged. Only `FinalizationPolicy` advances to revision `v2`, with immutable contextual tie-break definitions added to the specification.

When implementation and migration are published, 1.0 becomes `RETIRED` and 1.1 becomes the sole `AVAILABLE` version. Retirement prevents new bindings; it must not prevent existing 1.0 Sessions from resuming and completing with their original direct-pole semantics. Existing Sessions never auto-upgrade, and historical facts/results are never reinterpreted or backfilled with invented question/option IDs. This documentation change does not publish or activate 1.1.

See [ADR-0018](../adr/ADR-0018-contextual-tie-break-definition-version.md). Both decision sources remain valid history; `USER_TIE_BREAK` must not be renamed or removed.

## 11. Provenance Semantics

The goal is historical traceability and explainability, with reproducibility for deterministic assessment components.

A Session directly binds its exact `AssessmentDefinitionVersion`; the specification provides deterministic provenance for scoring, ambiguity evaluation, and finalization.

### AIProvenance MVP semantics

`AssessmentDefinitionVersion` carries the expected/default ClarificationPolicy revision as part of its immutable specification snapshot. `AIProvenance` belongs to each accepted `DimensionClarification` and records the actual effective successful AI execution that produced the **accepted** `ClarificationResult`.

At minimum it conceptually records:

- Model Identifier.
- Actual AI Clarification Policy revision/version.

It does **not** represent:

- Complete LLM execution history.
- All retry attempts.
- Every model/provider used in every conversation turn.
- Complete provider request/response logs.
- A persisted transcript.

If future audit requirements justify full execution history, a separate concept such as `ClarificationAttempt[]` can be introduced. MVP does not model it.

## 12. AI Clarification Persistence Principle

AI conversation content is not authoritative Assessment history.

Temporary/ephemeral runtime state may support multi-turn clarification, but its technical representation is deferred to specialist AI/Backend design.

MVP does not guarantee exact mid-conversation resume. If temporary context is lost before an accepted result is persisted:

- Session integrity remains intact.
- Submitted questionnaire remains frozen.
- InitialResult remains unchanged.
- Current dimension can restart clarification.
- Only a later accepted `ClarificationResult` plus required provenance/summary becomes durable business history.

## 13. Historical Assessment Deletion

Completed assessment content is immutable while the record exists, but the owner has the business right to delete a historical assessment.

MVP uses **hard deletion** of the owned historical `AssessmentSession` and its Session-dependent persisted data (including Clarification and tie-break rows). A database cascade may perform physical cleanup even though `DimensionClarification` is a separate DDD Aggregate. Before deletion, cross-module orchestration ends every ACTIVE Group Share that references the Session using reason `ASSESSMENT_DELETED`. Group retains the ended consent/share history and the historical opaque AssessmentSession identifier; it does not retain Assessment private content.

Account deletion is a separate deferred capability and requires its own future deletion/anonymization policy.

## 14. Open Questions

- Exact business-level clarification/provenance fields exposed in Result/History UI, within the already-frozen privacy boundary that excludes provider/model/prompt execution metadata.
- Temporary multi-turn AI runtime-context implementation.
- Detailed future account-deletion/anonymization policy.

The following are now frozen: `ABANDONED` source states, one-AVAILABLE-version MVP rule, explicit old-Session Restart targeting and retry semantics, replacement binding to the current AVAILABLE DefinitionVersion, ClarificationPolicy provenance binding, sharing-consent ownership/granularity, and historical Assessment hard-delete semantics.
