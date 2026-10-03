# Assessment Clarification Workflow Checkpoint

> **Checkpoint:** Assessment Step 7 complete
> **Date:** 2026-09-30
> **Status:** Accepted after Domain, Application, PostgreSQL integration, Web MVC, security and full regression tests

## 1. Milestone

Step 7 completes the deterministic Clarification/Tie-break workflow boundary around the already-completed questionnaire/scoring core.

Implemented Assessment flow now includes:

```text
Catalog
  ↓
Start / Resume
  ↓
Session-bound Questionnaire
  ↓
Autosave
  ↓
Submit final snapshot
  ↓
Deterministic scoring + ambiguity evaluation
  ↓
Immediate deterministic completion
OR
Create PENDING DimensionClarification rows
  ↓
Clarification lifecycle / Skip / Retry boundary
  ↓
Exact-tie user decision when required
  ↓
Deterministic post-clarification finalization
  ↓
Completed History / Historical Detail
```

Real LLM provider/runtime interaction remains Step 8.

## 2. DimensionClarification Aggregate

`DimensionClarification` is implemented as a separate Assessment Aggregate Root coordinated with `AssessmentSession` under ADR-0016.

Lifecycle:

```text
PENDING
  -> IN_PROGRESS
  -> CLARIFIED
  -> or FAILED_RETRYABLE -> IN_PROGRESS
  -> or SKIPPED
```

`CLARIFIED` may contain:

```text
RESOLVED + suggestedPole
UNCLEAR + no suggestedPole
```

Technical failure never becomes `UNCLEAR`.

Accepted clarification facts include:

```text
ClarificationResult
AIProvenance
startedAt
acceptedAt
updatedAt
```

## 3. Persistence lifecycle

PostgreSQL persistence now round-trips every Clarification lifecycle state and accepted result/provenance.

Durable invariants include:

```text
UNIQUE(session_id, dimension_code)
```

and:

```text
at most one IN_PROGRESS clarification per Session
```

Step 7C adds Flyway V5:

```text
V5__add_clarification_execution_token.sql
```

The row has an opaque `active_execution_token` exactly while `status = IN_PROGRESS`.

## 4. Skip and deterministic finalization

Step 7B implements:

```text
Skip one DimensionClarification
Skip all remaining Clarifications
Submit explicit exact-tie decision
```

Finalization is deterministic and uses only accepted/persisted business facts.

Representative rules:

```text
non-ambiguous
-> QUESTIONNAIRE

ambiguous + clarification agrees
-> QUESTIONNAIRE_CONFIRMED_BY_CLARIFICATION

ambiguous + clarification flips
-> AI_CLARIFICATION

UNCLEAR/SKIPPED + non-zero baseline
-> QUESTIONNAIRE_FALLBACK

exact tie + RESOLVED clarification
-> AI_CLARIFICATION

exact tie + UNCLEAR/SKIPPED
-> explicit persisted user tie-break required
-> USER_TIE_BREAK
```

`PENDING`, `IN_PROGRESS` and `FAILED_RETRYABLE` are not finalizable states.

## 5. Tie-break semantics

A tie-break is not a general user override.

It is valid only when:

```text
questionnaire result is an exact tie
AND
the dimension is ambiguous
AND
clarification is SKIPPED or CLARIFIED/UNCLEAR
AND
there is still no final preference
```

The selected pole must be valid for the exact bound `DimensionDefinition`.

The tie-break fact remains persisted separately from the derived `FinalAssessmentResult`.

## 6. Durable external-execution boundary

Step 7C establishes the internal two-transaction boundary required for future external AI work.

```text
Transaction A
  lock/revalidate Session
  lock/revalidate Clarification
  enter IN_PROGRESS
  create new active execution token
  commit

External work
  no database transaction / row lock

Transaction B
  lock/reload Session
  lock/reload Clarification
  verify current state
  verify execution token
  accept result/failure OR discard stale completion
  possibly finalize
  commit
```

This protects both:

```text
Restart / abandonment while external work is running
```

and the subtler retry race:

```text
execution A fails
retry starts execution B
late result A arrives while row is IN_PROGRESS again
```

A different active token makes A stale.

The token is workflow coordination metadata, not public API, AI provenance or complete attempt history.

## 7. Technical failure and retry

Technical provider failure semantics are already represented even though the real provider is not connected yet:

```text
IN_PROGRESS
-> FAILED_RETRYABLE
```

The Session returns from:

```text
CLARIFICATION_IN_PROGRESS
-> AWAITING_CLARIFICATION
```

The user may later Retry or Skip.

Retry stays inside the same logical `Session + Dimension` Clarification Aggregate and creates a new active execution token.

## 8. Deterministic HTTP boundary

Step 7D exposes the capabilities that do not require a live AI runtime:

```text
POST /api/v1/assessment-sessions/{sessionId}/clarifications/{dimensionCode}/skip

POST /api/v1/assessment-sessions/{sessionId}/clarifications/skip-remaining

PUT  /api/v1/assessment-sessions/{sessionId}/tie-breaks/{dimensionCode}
```

Controllers derive the acting `UserId` from the authenticated principal.

Unsafe requests remain CSRF-protected.

Private/missing AssessmentSession ownership uses the existing privacy-safe `404 ASSESSMENT_SESSION_NOT_FOUND` behavior.

Business-semantic failures use `422`, including:

```text
CLARIFICATION_NOT_ALLOWED
TIE_BREAK_NOT_REQUIRED
INVALID_DIMENSION_TIE_BREAK
```

## 9. Deliberately deferred public AI interaction

The following design-frozen routes are not yet backed by the real runtime interaction:

```text
Start clarification
Continue clarification
Retry clarification
```

The internal Application execution boundary exists, but exposing a public endpoint that only returns an internal execution ticket would leak implementation detail and create a fake half-complete UX.

Step 8 will connect those intents to the actual AI capability boundary and return runtime assistant interaction/output.

## 10. Test coverage

Step 7 is accepted after the complete backend test suite passed.

Coverage includes:

```text
Domain lifecycle transitions and illegal transitions
ClarificationResult invariants
AIProvenance persistence
PENDING / IN_PROGRESS / CLARIFIED / SKIPPED / FAILED_RETRYABLE round-trip
PostgreSQL one-IN_PROGRESS invariant
Skip one / Skip remaining
non-zero questionnaire fallback
exact-tie detection
tie-break persistence
USER_TIE_BREAK final decision source
automatic deterministic finalization
same-value tie-break retry/recovery
different-value historical rewrite rejection
execution-token persistence
retry creates a new token
old-token stale result discard
Restart/abandonment stale result discard
HTTP mapping
authentication
CSRF
privacy-safe 404
422 business-semantic errors
```

The remaining testing gap is not ordinary Step 7 correctness; it is dedicated simultaneous-transaction stress coverage for a few concurrency races. This can be added alongside Step 8/provider integration rather than blocking the accepted Step 7 milestone.

## 11. Review conclusions

Accepted:

```text
DimensionClarification = separate Aggregate Root
AssessmentSession = Session-wide lifecycle/concurrency anchor
one logical Clarification per Session + Dimension
one IN_PROGRESS Clarification per Session
no DB lock across external work
opaque active execution token protects retry races
technical failure != UNCLEAR
tie-break is exact-tie-only
post-clarification Finalization is deterministic
public Skip/Tie-break endpoints expose user intent, not state mutation
```

No new architectural blocker was found in Step 7 review.

## 12. Documentation alignment

Step 7 review updates:

```text
backend README / current checkpoint
Application transaction boundary
PostgreSQL Clarification persistence
Flyway history through V5
REST implementation status + error contract
OpenAPI v0.4.0
implementation handoff
testing strategy
roadmap
Assessment Domain active-execution invariant
ADR-0016 implementation refinement
```

Historical checkpoint documents remain unchanged.

## 13. Remaining Assessment work

### Step 8 — real LLM integration

Still required:

```text
provider abstraction / adapter
prompt + clarification policy implementation
structured-output parsing/validation
temporary multi-turn runtime context
provider timeout / transport-error mapping
actual AIProvenance capture
Start / Continue / Retry public runtime interaction
privacy/safety review
```

The existing Step 7 execution boundary should remain stable while providers are added.

### Historical Assessment deletion

Still deferred until Group sharing exists, because deletion must first end every ACTIVE share referencing the historical Session with reason `ASSESSMENT_DELETED`.

### Final Assessment documentation / API review

After Step 8, perform a final Assessment checkpoint and synchronize any provider-facing implementation feedback without allowing provider concepts to leak into the Assessment Domain.

## 14. Interview explanation

A concise explanation:

> I modeled each ambiguous-dimension clarification as a separate Aggregate, but kept the AssessmentSession as the workflow and finalization concurrency anchor. External AI work is split across two short database transactions. An opaque execution token protects against a subtle stale-result race where an older provider call returns after the same clarification has already been retried and is IN_PROGRESS again. Final personality results remain deterministic: AI contributes only an accepted dimension-level clarification fact, while the backend applies explicit fallback, exact-tie and finalization rules.

## 15. Post-checkpoint full-stack integration corrections

Frontend F5/F6 integration exercised the Step 7 HTTP boundary against the real Spring Boot runtime and exposed one backend mapping defect without changing the accepted Domain/REST contract.

For a completed exact-tie dimension, `InitialDimensionResult.questionnairePreference` is legitimately `null` while the final preference may come from `USER_TIE_BREAK`. The original Web mapper projected the nullable preference inside a Java Stream and then called `findFirst()`, allowing a null stream element to trigger a `NullPointerException` after deterministic finalization had already succeeded.

The resulting failure mode was important:

```text
PUT final tie-break
  -> Domain/Application finalization succeeds
  -> transaction commits COMPLETED Session
  -> Web response mapping encounters null questionnaire baseline
  -> HTTP 500

later GET same Session
  -> persisted Session is still COMPLETED
  -> same response mapper fails again
  -> HTTP 500
```

The mapper now finds the matching initial dimension first and then maps the nullable preference through `Optional`, so exact-tie history can expose:

```text
questionnairePreference = null
finalPreference = <selected pole>
source = USER_TIE_BREAK
overrodeBaseline = false
```

A dedicated regression test covers the completed exact-tie Web mapping. No OpenAPI version bump is required because the public schema already allowed the nullable questionnaire preference; the implementation was corrected to honor the existing contract.

## 16. Project sequencing update

Step 8 remains required, but it is no longer the immediate project workstream. After Frontend F6 completed the executable deterministic slice, the active focus moved to containerization, AWS deployment, Terraform and GitHub Actions CI/CD.

Historical Assessment deletion remains deferred until the Group sharing boundary exists. Provider-backed LLM interaction is intentionally postponed until after the Cloud/CI-CD line is established.

