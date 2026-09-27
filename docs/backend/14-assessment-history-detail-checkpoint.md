# Assessment History + Historical Detail Checkpoint

> **Checkpoint:** Assessment Step 6 complete  
> **Date:** 2026-09-27  
> **Status:** Accepted after Application, PostgreSQL integration, Web MVC, and full regression tests

## 1. Milestone

Step 6 completes the Assessment read side for completed-history browsing and historical Session detail.

The Assessment backend now supports the core user journey through:

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
Deterministic scoring
  ↓
Immediate completion OR clarification setup
  ↓
Restart / Start New
  ↓
Completed History
  ↓
Historical Session Detail
```

The important architectural change in Step 6 is that historical workflow state is no longer reconstructed from questionnaire ambiguity alone. Persisted Clarification and Tie-break facts are now authoritative for Session detail.

---

## 2. Step 6A — Completed History

The public history capability is:

```text
GET /api/v1/assessment-sessions
    ?status=COMPLETED
    &page=1
    &size=20
```

Current MVP semantics:

```text
History = completed AssessmentSessions only
```

`ABANDONED`, `IN_PROGRESS`, `AWAITING_CLARIFICATION`, and `CLARIFICATION_IN_PROGRESS` are not part of the completed-result history.

Pagination is:

```text
page starts at 1
default size = 20
maximum size = 100
```

Ordering is deterministic:

```text
completed_at DESC
session id DESC as a stable tie-break
```

A page beyond the available range returns an empty `items` collection rather than 404.

---

## 3. Purpose-built History Projection

History does not hydrate full `AssessmentSession` Aggregates.

The read path is:

```text
AssessmentHistoryController
        ↓
GetAssessmentHistoryService
        ↓
AssessmentHistoryQuery
        ↓
PostgresAssessmentHistoryQuery
        ↓
lightweight PostgreSQL projection
```

The list projection contains only:

```text
sessionId
assessmentCode
assessmentVersion
finalType
completedAt
```

It intentionally does not load:

```text
questionnaire answers
InitialAssessmentResult
Clarifications
Tie-breaks
full FinalDimensionConclusion detail
```

Those belong to Session detail.

This keeps query cost aligned with the UI use case and demonstrates a deliberate separation between mutation-oriented Aggregate repositories and optimized read projections without introducing a separate CQRS subsystem.

---

## 4. Historical Result Immutability

History reads the already-persisted durable `FinalAssessmentResult`.

For the current implementation:

```text
final_result ->> 'finalType'
```

is read directly from PostgreSQL JSONB.

History does not:

```text
reload questionnaire answers
re-run deterministic scoring
re-run clarification
re-compose the result using current rules
```

Therefore a completed result remains tied to the exact historical `AssessmentDefinitionVersion` that produced it.

---

## 5. Step 6B — Historical Detail Hardening

The existing detail endpoint remains:

```text
GET /api/v1/assessment-sessions/{sessionId}
```

A separate `/history/{id}` endpoint is not needed.

The detail path now combines:

```text
owner-scoped AssessmentSession
+
exact bound AssessmentDefinitionVersion
+
persisted clarification workflow facts
+
persisted tie-break facts
```

into the owner-facing `AssessmentSessionResponse`.

---

## 6. Persisted Workflow Is Authoritative

Before Step 6, post-submission read behavior could temporarily infer:

```text
InitialDimensionResult.ambiguous = true
→ clarification is PENDING
```

That approximation was sufficient only while `PENDING` was the sole implemented clarification state.

Step 6 removes that transitional assumption.

The authoritative read source is now:

```text
assessment_dimension_clarifications
assessment_dimension_tie_breaks
```

The read-side port is:

```text
AssessmentSessionWorkflowQuery
```

implemented by:

```text
PostgresAssessmentSessionWorkflowQuery
```

It produces:

```text
AssessmentSessionWorkflowSnapshot
├── ClarificationSnapshot[]
└── TieBreakSnapshot[]
```

The Session read model then derives public workflow convenience fields from those persisted facts.

---

## 7. Clarification Detail Supported by the Read Model

The current historical read model can represent persisted clarification states:

```text
PENDING
IN_PROGRESS
CLARIFIED
SKIPPED
FAILED_RETRYABLE
```

For a `CLARIFIED` dimension it can also represent:

```text
resolution = RESOLVED | UNCLEAR
suggestedPole
confidence
reasoningSummary
startedAt
acceptedAt
```

This does not mean the mutation-side Clarification workflow is implemented yet. It means historical/query infrastructure is ready to faithfully read those states once Step 7 begins writing them.

---

## 8. Tie-break Detail Supported by the Read Model

Persisted tie-break rows are read from:

```text
assessment_dimension_tie_breaks
```

and exposed as:

```text
dimensionCode
selectedPole
decidedAt
```

The read model can therefore represent an exact-tie path without reconstructing a user choice from other data.

---

## 9. Actionable Workflow vs Historical Facts

Step 6 explicitly distinguishes:

```text
historical persisted facts
```

from:

```text
actions still available to the user
```

For example, an abandoned Session may retain:

```text
clarification = PENDING
```

as a historical fact because the clarification row existed before Restart.

However an `ABANDONED` Session must not expose that dimension as an actionable:

```text
workflow.pendingClarificationDimensions
```

because the attempt is terminal.

Therefore terminal Sessions return no actionable workflow work:

```text
COMPLETED
→ workflow.completed = true

ABANDONED
→ workflow.completed = false
   but no pending/retryable/tie-break-required actions
```

This prevents historical detail from misleading the frontend into continuing a terminated attempt.

---

## 10. Exact-Version Historical Interpretation

Historical detail continues to resolve:

```text
session.definitionVersionId
```

directly.

It does not use:

```text
current AVAILABLE DefinitionVersion
```

This remains essential because historical questionnaire evidence and dimension interpretation must be explained using the exact specification bound to the original attempt.

---

## 11. Owner Scoping and Privacy

History is scoped entirely by the authenticated `UserId`.

Session detail remains owner-scoped.

A missing Session and another user's guessed Session both resolve to the same privacy-safe:

```text
404 ASSESSMENT_SESSION_NOT_FOUND
```

The frontend cannot supply an arbitrary owner id to expand access.

---

## 12. History Validation

The API currently accepts only:

```text
status=COMPLETED
```

Unsupported history statuses return:

```text
400 VALIDATION_FAILED
```

Invalid pagination also returns:

```text
400 VALIDATION_FAILED
```

Application-level pagination validation remains in place even though HTTP also supplies defaults, so the use case is protected independently of the transport layer.

---

## 13. PostgreSQL Design

No Step 6 migration was required.

The existing V3 history index is used by the completed-history access pattern:

```text
(user_id, completed_at DESC)
WHERE status = 'COMPLETED'
```

The existing clarification and tie-break tables already contained the durable workflow fields required by historical detail.

This validates the earlier schema decision to persist workflow facts independently rather than embedding all clarification state inside the Session JSON result.

---

## 14. Test Coverage

Step 6 is accepted after the full test suite passed.

Important Step 6 coverage includes:

### Application

```text
History pagination validation
owner-scoped history query delegation
exact bound DefinitionVersion for detail
workflow snapshot composition
active/resume reads using persisted workflow state
```

### PostgreSQL integration

```text
completed-only history
owner isolation
completedAt descending pagination
history metadata
authoritative persisted clarification read
authoritative persisted tie-break read
```

The key historical-detail regression case mutates real persisted workflow state to values such as:

```text
FAILED_RETRYABLE
CLARIFIED / UNCLEAR
persisted tie-break
```

and verifies that Session detail returns those database facts rather than reconstructing `PENDING` from the InitialResult.

### Web MVC

```text
history endpoint
default pagination
explicit pagination
invalid status
invalid page / size
authentication
historical Session detail mapping
clarification result mapping
tie-break mapping
```

### Regression

The complete backend test suite passes after the Step 6 read-side changes.

---

## 15. Review Findings

### Accepted architecture

The following decisions are accepted:

```text
History uses a purpose-built read projection
Detail reuses the existing Session resource
persisted Clarification/Tie-break data is authoritative
exact DefinitionVersion remains authoritative
terminal historical facts are separated from actionable workflow
owner scoping remains enforced server-side
no new schema was required
```

### Design debt closed

The Step 4 transitional behavior:

```text
ambiguous InitialResult
→ infer PENDING clarification
```

is now closed.

Future clarification states can be read without changing the historical Session endpoint architecture.

### Remaining read-model consideration

The database already persists AI provenance fields such as provider, model identifier, and clarification policy revision.

Step 6 deliberately does not expose those internal execution fields through the public Session DTO. The privacy boundary is already frozen: provider/model/prompt execution metadata remain internal.

Step 7/8 may decide which **business-level** clarification/provenance fields (for example decision source or a safe result summary) belong in a future Result/History DTO, but that decision must remain inside the frozen privacy boundary above.

### Pagination choice

Offset pagination is sufficient for the current MVP and matches the frozen API design.

Do not introduce keyset/cursor pagination unless real scale or UX requirements justify it.

---

## 16. Assessment Work Remaining

The deterministic Assessment core and read side are now largely complete.

### Step 7 — Clarification + Tie-break Workflow

Implement the mutation-side lifecycle that the Step 6 read model is already able to represent.

Expected capabilities include:

```text
start one pending dimension clarification
PENDING -> IN_PROGRESS

accept clarification result
IN_PROGRESS -> CLARIFIED

handle retryable AI failure
IN_PROGRESS -> FAILED_RETRYABLE

retry a failed clarification
FAILED_RETRYABLE -> IN_PROGRESS

skip clarification where allowed
PENDING / FAILED_RETRYABLE -> SKIPPED

determine whether another ambiguous dimension remains

detect unresolved exact ties

record explicit user tie-break

finalize automatically when every ambiguous dimension is resolved
```

Critical invariants:

```text
only ambiguous dimensions are clarifiable
at most one clarification is IN_PROGRESS per Session
InitialAssessmentResult is never rewritten
exact tie must not silently choose a pole
FinalAssessmentResult is created exactly once
```

### Step 8 — AI Integration

Connect the clarification lifecycle to an AI provider behind an Application/Infrastructure boundary.

Expected concerns:

```text
provider abstraction
clarification prompt construction
ClarificationPolicy revision
model identifier
AI provenance persistence
structured output validation
timeout / provider failure
FAILED_RETRYABLE behavior
post-call Session revalidation
discard stale AI output after Restart / abandonment
no AI access to deterministic scoring
```

The AI integration must remain subordinate to the deterministic Assessment workflow rather than becoming the owner of the final result.

### Historical Assessment Deletion

The REST/domain design includes deletion of completed Assessment history, but this is intentionally not implemented yet.

Deletion is cross-module orchestration because a completed Assessment may later be referenced by active Group shares.

The final flow must coordinate:

```text
end every ACTIVE GroupAssessmentShare
with reason = ASSESSMENT_DELETED

then hard-delete the owned historical AssessmentSession
```

Because Group sharing is not implemented yet, completing this use case before the Group module would either create a fake dependency or require rework. Keep it deferred until the Group sharing boundary exists.

### API / Documentation Synchronization

Restart, History, and Session Detail are now synchronized in `docs/api/openapi.yaml` v0.3.0. Before declaring the entire Assessment module finished:

```text
revalidate/update docs/api/openapi.yaml
against the implemented Step 7 clarification/tie-break behavior
and Step 8 AI integration contract

update REST / Domain / Application docs after Step 7/8

record the final Assessment architecture checkpoint
```

---

## 17. Assessment Completion Outlook

After Step 6, the implemented Assessment capabilities are:

```text
Definition / Version
Catalog
Start / Resume
one-active-session invariant
Session-bound questionnaire
Autosave
Submit final snapshot
deterministic scoring
InitialAssessmentResult
ambiguity detection
immediate deterministic finalization
FinalAssessmentResult persistence
PENDING clarification setup
Restart / Start New
concurrency-safe replacement
Session detail
completed history
authoritative persisted workflow detail
```

The major unfinished business capability is now:

```text
actually resolving ambiguous dimensions
```

Everything around deterministic questionnaire execution, version binding, submission, history, recovery, and read-side reproducibility is already in place.

---

## 18. Interview Explanation

A concise explanation of Step 6:

> I separated the Assessment command model from its read needs without introducing a separate CQRS system. Completed history uses a lightweight PostgreSQL projection, while Session detail combines the owner-scoped Session with the exact bound DefinitionVersion and persisted clarification/tie-break facts. This avoids rehydrating full aggregates for list queries and prevents historical workflow state from being reconstructed incorrectly from the original questionnaire result.

---

## 19. Suggested Commit

```text
feat(assessment): add history and harden session detail reads
```

Suggested body:

```text
- add completed assessment history with pagination
- use a lightweight PostgreSQL history projection
- keep completed history owner-scoped and version-aware
- read clarification and tie-break state from persisted workflow facts
- remove derived pending-clarification behavior from session detail
- distinguish terminal historical facts from actionable workflow state
- harden active/resume reads to use authoritative workflow data
- add history, workflow projection, and historical detail tests
```
