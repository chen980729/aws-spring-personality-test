# Assessment Domain Model

> **Status:** Accepted Assessment Domain Baseline — implementation-aligned through Step 5  
> **Last updated:** 2026-09-27

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
├── DimensionClarification[] (Entity)
│   ├── ClarificationResult (VO)
│   └── AIProvenance (VO)
└── FinalAssessmentResult (VO)
    └── FinalDimensionConclusion[] (VO)
```

`DimensionClarification` remains inside the Session Aggregate for MVP because eligibility and completion readiness depend directly on the session's immutable initial evidence and unresolved ambiguous dimensions.

### 7.2 AssessmentDefinitionVersion Aggregate

```text
AssessmentDefinitionVersion (Entity / Aggregate Root)
├── QuestionnaireDefinition (VO)
│   └── QuestionDefinition[] (VO)
├── DimensionDefinition[] (VO)
├── ScoringPolicy (VO)
├── AmbiguityPolicy (VO)
└── FinalizationPolicy (VO)
```

Once executable/available to users, this specification is immutable.

### 7.3 AssessmentDefinition Aggregate

`AssessmentDefinition` remains a small provisional Aggregate Root representing the long-lived assessment identity and potential future version-governance boundary. It must not receive artificial behavior merely to justify Aggregate status.

## 8. AssessmentSession Aggregate Invariants

### AS-INV-01 Stable Session Context

Session owner and bound `AssessmentDefinitionVersion` never change.

### AS-INV-02 Response Replacement Boundary

Before questionnaire submit, the Session may replace its `QuestionnaireResponse` with a new immutable value. After submit, it may not.

### AS-INV-03 Initial Result Creation

`InitialAssessmentResult` can be created only after questionnaire submission and at most once.

### AS-INV-04 Initial Evidence Preservation

`InitialAssessmentResult` cannot be replaced or modified. AI clarification cannot rewrite it.

### AS-INV-05 Clarification Eligibility

Only dimensions already classified as ambiguous in the `InitialAssessmentResult` are eligible for clarification.

### AS-INV-06 One Clarification Lifecycle per Dimension

At most one logical `DimensionClarification` exists per `Session + Dimension`. Retries remain within that lifecycle.

### AS-INV-07 Single Active Clarification (MVP)

At most one `DimensionClarification` may be `IN_PROGRESS` in an `AssessmentSession` at a time. A different eligible dimension may start clarification only when no other clarification is actively `IN_PROGRESS`. Parallel clarification conversations are not supported in MVP.

### AS-INV-08 Clarification State Consistency

- `CLARIFIED` requires a valid accepted `ClarificationResult` (`RESOLVED` with a pole, or `UNCLEAR` with no pole).
- `SKIPPED` must not imply an AI judgment was produced.
- Local state and accepted outcome must not contradict each other.

### AS-INV-09 Completion Readiness

All ambiguous dimensions must be `CLARIFIED` or `SKIPPED` before completion. If there were no ambiguous dimensions, this condition is naturally satisfied.

### AS-INV-10 Completion Consistency

A legal completion transition produces exactly one `FinalAssessmentResult` and enters `COMPLETED`. Completed assessment business content cannot be edited in place.

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

## 10. Versioning and Governance Rules

### VER-01 Semantic Change Requires a New Version

Any semantic change to questionnaire content, dimensions, scoring, ambiguity rules, or finalization behavior requires a new `AssessmentDefinitionVersion` once the existing version is executable/available to users.

### VER-02 Technical Refactor Does Not Require a New Domain Version

Implementation refactors, performance improvements, or SQL changes that preserve assessment semantics do not create a new assessment version.

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

MVP uses **hard deletion** of the owned historical `AssessmentSession` and its Session-owned child data. Before deletion, cross-module orchestration ends every ACTIVE Group Share that references the Session using reason `ASSESSMENT_DELETED`. Group retains the ended consent/share history and the historical opaque AssessmentSession identifier; it does not retain Assessment private content.

Account deletion is a separate deferred capability and requires its own future deletion/anonymization policy.

## 14. Open Questions

- User-facing amount of deterministic/AI provenance in Result/History UI.
- Temporary multi-turn AI runtime-context implementation.
- Detailed future account-deletion/anonymization policy.

The following are now frozen: `ABANDONED` source states, one-AVAILABLE-version MVP rule, explicit old-Session Restart targeting and retry semantics, replacement binding to the current AVAILABLE DefinitionVersion, ClarificationPolicy provenance binding, sharing-consent ownership/granularity, and historical Assessment hard-delete semantics.
