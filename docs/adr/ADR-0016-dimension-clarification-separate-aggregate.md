# ADR-0016: Model DimensionClarification as a Separate Assessment Aggregate

- Status: Accepted
- Date: 2026-09-28
- Supersedes: ADR-0007

## Context

The original MVP design placed `DimensionClarification` inside the `AssessmentSession` Aggregate because clarification eligibility and finalization readiness depend on Session state.

Implementation Steps 4-6 exposed a more useful consistency boundary:

- `assessment_dimension_clarifications` is durable relational workflow state with unique `(session_id, dimension_code)` identity.
- the Application layer already persists/reads Clarification through a dedicated Repository abstraction.
- one Clarification may move through `PENDING -> IN_PROGRESS -> CLARIFIED / SKIPPED / FAILED_RETRYABLE` independently of questionnaire/result snapshots.
- the real AI flow must never hold a database transaction or Session lock while waiting for an external LLM call.
- accepted AI result/provenance is durable, while conversational runtime context is temporary and non-authoritative.

Keeping all Clarification state inside one in-memory `AssessmentSession` Aggregate would either require loading a growing child collection for every Session mutation or would make the documented Aggregate boundary differ from the actual transaction/repository boundary.

## Decision

`DimensionClarification` is a separate Aggregate Root inside the Assessment module for the MVP.

It is not independently user-owned and is always scoped by:

```text
AssessmentSessionId + DimensionCode
```

`AssessmentSession` remains the workflow/concurrency anchor for operations that can affect Session status or finalization.

Clarification commands therefore use short local transactions such as:

```text
transaction A
  lock AssessmentSession
  revalidate ownership + Session state
  load target DimensionClarification
  validate eligibility / single-active rule
  transition Clarification -> IN_PROGRESS
  transition Session -> CLARIFICATION_IN_PROGRESS where required
  commit

external LLM call
  no DB transaction / row lock

transaction B
  lock AssessmentSession
  reload target DimensionClarification
  revalidate Session + Clarification state
  accept CLARIFIED or FAILED_RETRYABLE result
  determine next workflow state / finalization readiness
  persist both Aggregates
  commit
```

Cross-Aggregate workflow invariants are protected by a combination of:

- Domain behavior on each Aggregate.
- Assessment Application orchestration.
- short `AssessmentSession` row locks for Clarification/finalization mutations.
- unique `(session_id, dimension_code)` for one logical lifecycle per dimension.
- partial unique index allowing at most one `IN_PROGRESS` Clarification per Session.
- post-LLM reload/revalidation so late results cannot mutate an abandoned or otherwise stale Session.

### Implementation refinement — active execution token (2026-09-30)

Step 7 implementation exposed one additional stale-result race: after execution A fails and the same Clarification is retried as execution B, the row may again be `IN_PROGRESS` when a late result from A arrives. Session/status revalidation alone cannot distinguish A from B.

The implemented refinement is therefore:

```text
enter/re-enter IN_PROGRESS
-> generate a new opaque active execution token

external work
-> carries the internal execution ticket/token

completion transaction
-> reload Session + Clarification
-> require token == current active token
-> otherwise discard as stale
```

The token is cleared on accepted result, retryable technical failure, or Skip. It is not a `ClarificationAttempt[]` audit log and is not exposed as public API/provenance. This refines the consistency mechanism without changing the Aggregate boundary decided by this ADR.

## Consequences

### Positive

- the Domain/repository model matches the persisted lifecycle already implemented.
- Step 7 can perform long external AI work without keeping one large Aggregate/transaction open.
- Clarification retry and accepted-result provenance have a clear persistence boundary.
- Session finalization remains serialized through the Session concurrency anchor.
- read-side projections can load Clarification state independently without reconstructing it from `InitialAssessmentResult`.

### Trade-offs

- some business rules now span two Aggregates and must be coordinated by the Application layer.
- correctness depends on explicit locking/constraint rules; an in-memory Aggregate alone no longer protects all Session-wide Clarification invariants.
- developers must not mutate a Clarification in isolation when the operation can change Session status or completion readiness.

## Alternatives considered

### Keep Clarification inside AssessmentSession

This preserves a simpler textbook Aggregate model but no longer matches the implemented persistence/repository boundary and is awkward for the two-transaction external LLM workflow.

### Make the workflow eventually consistent

Not chosen for this MVP. The application is a single PostgreSQL-backed Modular Monolith, and Session/Clarification transitions are small local writes. Short local transactions plus targeted locking are simpler and give stronger user-visible consistency without introducing events/Sagas.

## Implementation rule

A dedicated `DimensionClarificationRepository` is valid because `DimensionClarification` is now an Aggregate Root. Operations affecting finalization or Session lifecycle must still coordinate through the Assessment Application layer and lock/revalidate the parent `AssessmentSession` as defined above.
