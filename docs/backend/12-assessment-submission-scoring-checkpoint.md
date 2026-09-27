# Assessment Submission + Deterministic Scoring Checkpoint

## 1. Checkpoint

**Assessment Start/Resume + Autosave + Submission + Deterministic Scoring Complete**

This checkpoint closes the deterministic core Assessment workflow before Restart, History, AI Clarification, Tie-break, and clarification-driven finalization.

```text
Authenticated User
    ↓
Assessment Catalog
    ↓
Start / Resume AssessmentSession
    ↓
Get Session-bound Questionnaire
    ↓
Autosave Questionnaire Snapshot
    ↓
Submit Final Questionnaire Snapshot
    ↓
Deterministic Scoring
    ↓
Ambiguity Evaluation
    ├── no ambiguous dimensions
    │       ↓
    │   FinalAssessmentResult
    │       ↓
    │    COMPLETED
    │
    └── ambiguous dimensions exist
            ↓
      PENDING DimensionClarification rows
            ↓
      AWAITING_CLARIFICATION
```

Submission is an irreversible business boundary.

## 2. Scope Completed

Completed in this checkpoint:

```text
AssessmentDefinition / AssessmentDefinitionVersion
Assessment Catalog
AssessmentSession Start / Resume
one-active-session invariant
exact DefinitionVersion binding
Session-bound questionnaire read
Questionnaire autosave
complete snapshot replacement semantics
semantic answer validation
optimistic locking
final questionnaire submission
complete submission validation
deterministic scoring
InitialAssessmentResult
ambiguity classification
immediate deterministic finalization
FinalAssessmentResult
PENDING clarification setup
full Session response after submission
authoritative Session detail read
submission retry / recovery contract
```

Explicitly deferred:

```text
Restart / Start New
Assessment History list
Assessment deletion
AI Clarification execution
Clarification retry / skip
Tie-break
clarification-driven FinalAssessmentResult
Group result sharing
```

## 3. Submission Boundary

Public command:

```text
POST /api/v1/assessment-sessions/{sessionId}/questionnaire/submission
```

The request carries the complete final questionnaire snapshot again instead of relying on the last autosave.

```text
autosave in flight: Q48 = 4
user submits:       Q48 = 5

authoritative submitted answer: Q48 = 5
```

The same submitted snapshot is used for validation, scoring, and persistence. After submission, QuestionnaireResponse is frozen, `questionnaireSubmittedAt` is durable, and `InitialAssessmentResult` is durable. Re-submission is rejected.

## 4. Atomic Submit Transaction

`SubmitQuestionnaireService` owns the transaction:

```text
@Transactional

load owned Session
    ↓
load exact bound DefinitionVersion
    ↓
validate final snapshot
    ↓
build canonical QuestionnaireResponse
    ↓
deterministic scoring
    ↓
InitialAssessmentResult
    ↓
ambiguity evaluation
    ↓
optional immediate FinalAssessmentResult
    ↓
AssessmentSession.submit(...)
    ↓
persist Session state
    ↓
create PENDING clarification rows if required
    ↓
COMMIT
```

The system must not persist intermediate states such as submitted-without-initial-result or awaiting-clarification-without-clarification-lifecycle.

## 5. Deterministic Scoring

`QuestionnaireScoringService` is a pure Domain Service depending only on `AssessmentSpecification + QuestionnaireResponse`.

For `CENTERED_BALANCED_KEYING`:

```text
centeredScore = answerValue - answerCenter

question keyed to Pole A → +centeredScore
question keyed to Pole B → -centeredScore
```

Dimension interpretation:

```text
rawScore > 0 → Pole A preference
rawScore < 0 → Pole B preference
rawScore = 0 → exact tie, questionnairePreference = null
```

Ambiguity:

```text
abs(rawScore) <= inclusiveThreshold
```

The engine is DefinitionVersion-driven and does not hardcode EI/SN/TF/JP or question text.

## 6. InitialAssessmentResult

`InitialAssessmentResult` is immutable historical evidence:

```text
InitialAssessmentResult
└── InitialDimensionResult[]
    ├── dimension
    ├── rawScore
    ├── questionnairePreference
    └── ambiguous
```

It records what the submitted questionnaire itself determined. AI Clarification, Tie-break, and Finalization must never rewrite it.

## 7. FinalAssessmentResult

When no dimension is ambiguous, finalization is deterministic and immediate.

```text
InitialAssessmentResult
    ↓
AssessmentFinalizationService
    ↓
FinalAssessmentResult
```

Immediate finalization uses `decisionSource = QUESTIONNAIRE`. The final type is derived from ordered final dimension conclusions rather than supplied by the frontend.

A `COMPLETED` Session must have a `FinalAssessmentResult`.

## 8. Ambiguous Submission

If one or more dimensions are ambiguous:

```text
IN_PROGRESS
    ↓
AWAITING_CLARIFICATION
```

There is no immediate FinalAssessmentResult. The same transaction creates one `PENDING` DimensionClarification per ambiguous dimension. Actual AI clarification is deferred.

## 9. Persistence

`assessment_sessions` now actively uses:

```text
questionnaire_response JSONB
questionnaire_submitted_at
initial_result JSONB
final_result JSONB
completed_at
version
```

Persistence JSON models remain separate from Domain records:

```text
Domain Result VO
    ↓
AssessmentResultPersistenceMapper
    ↓
Persistence JSON model
    ↓
JsonNode
    ↓
PostgreSQL JSONB
```

Database CHECK constraints independently protect lifecycle consistency, including submission/initial-result pairing and the requirements for COMPLETED state.

## 10. Clarification Persistence Foundation

`assessment_dimension_clarifications` is now used at Submit time. Current implemented lifecycle state is `PENDING`. The schema already supports future `IN_PROGRESS`, `CLARIFIED`, `SKIPPED`, and `FAILED_RETRYABLE` states plus durable clarification outcome and AI provenance.

## 11. HTTP Session Representation

`AssessmentSessionResponse` now supports post-submission state and can expose:

```text
id
assessment code/version
status
questionnaire state
initialResult
clarifications
tieBreaks
finalResult
workflow
createdAt
completedAt
abandonedAt
```

The backend remains authoritative for whether clarification is required, whether the Session is complete, and what final result exists.

## 12. Authoritative Session Read

```text
GET /api/v1/assessment-sessions/{sessionId}
```

This is owner-scoped. Missing Session and another user's Session both return `404 ASSESSMENT_SESSION_NOT_FOUND`. It uses the exact DefinitionVersion bound to the Session, not the current AVAILABLE catalog version.

## 13. Submission Retry / Recovery Contract

```text
first Submit commits
    ↓
HTTP response is lost
    ↓
client retries Submit
    ↓
409 ASSESSMENT_ALREADY_SUBMITTED
    ↓
GET /api/v1/assessment-sessions/{sessionId}
    ↓
recover authoritative committed state
```

Scoring is not executed as a second accepted submission and no second InitialAssessmentResult is created.

## 14. Error Semantics

Important errors at this checkpoint:

```text
401 AUTHENTICATION_REQUIRED
403 CSRF_VALIDATION_FAILED
404 ASSESSMENT_SESSION_NOT_FOUND
409 ASSESSMENT_ALREADY_SUBMITTED
409 ASSESSMENT_SESSION_ALREADY_ABANDONED
409 ASSESSMENT_SESSION_CONCURRENT_MODIFICATION
422 QUESTIONNAIRE_INCOMPLETE
422 INVALID_QUESTIONNAIRE_RESPONSE
```

Autosave permits partial snapshots; `QUESTIONNAIRE_INCOMPLETE` is specific to submission.

## 15. Test Coverage

### Domain

```text
QuestionnaireResponse invariants
AssessmentSession submission lifecycle
submission cannot happen twice
answers freeze after submission
ambiguous result → AWAITING_CLARIFICATION
clear result → COMPLETED
QuestionnaireScoringService direction / tie / ambiguity boundary / determinism
AssessmentFinalizationService immediate finalization rules
```

### Application

```text
Autosave semantic validation
exact bound DefinitionVersion use
owner-scoped Session access
Get AssessmentSession exact-version read
```

### Persistence / PostgreSQL Testcontainers

```text
QuestionnaireResponse JSONB round trip
InitialAssessmentResult JSONB round trip
FinalAssessmentResult JSONB round trip
optimistic locking
ambiguous submission → initial_result + PENDING clarifications
clear submission → initial_result + final_result + COMPLETED
Flyway / JPA / PostgreSQL compatibility
```

### Web MVC

```text
questionnaire read
autosave
submission
Session detail read
authentication
CSRF
400 transport validation
404 privacy-safe Session lookup
409 resubmission
422 incomplete questionnaire
post-submission response mapping
```

## 16. Manual Verification

Runtime verification completed:

```text
Start Session
submit a complete 48-answer snapshot
submission succeeds
random answer set → AWAITING_CLARIFICATION
repeat submission → rejected
GET Session after submission → committed authoritative state
```

`AWAITING_CLARIFICATION` for a random answer set is valid whenever at least one dimension satisfies the configured ambiguity policy. The immediate `COMPLETED` branch is covered by automated integration tests; a dedicated manual clear-answer run is optional hardening, not a blocker for this checkpoint.

## 17. Architecture Review

### Strengths

```text
DefinitionVersion reproducibility
backend-owned scoring
pure deterministic Domain calculation
immutable InitialAssessmentResult
separate Initial / Final result concepts
atomic submission transaction
database lifecycle constraints
privacy-safe owner-scoped reads
retry recovery protocol
real PostgreSQL integration tests
clear Web / Application / Domain / Persistence boundaries
```

### Known design debt

#### 17.1 Clarification read model is transitional

At this checkpoint only `PENDING` clarification is implemented. Before AI Clarification begins, Session reads should be refactored to load persisted clarification rows as the authoritative source. This becomes mandatory once statuses can change to `IN_PROGRESS`, `FAILED_RETRYABLE`, `CLARIFIED`, or `SKIPPED`.

#### 17.2 Autosave and Submit duplicate answer validation

Both validate question existence, duplicates, allowed values, and canonical ordering; Submit additionally requires completeness. The duplication is acceptable now, but before adding more questionnaire commands it is worth extracting one focused reusable definition-driven response validator/builder. Avoid a large validation framework.

#### 17.3 Scoring/submission wording should stay precise

The implementation computes deterministic scoring inside the same Submit transaction immediately before the Aggregate accepts the submission facts. There is no persisted or externally observable “scored draft” state. Documentation should therefore say: `InitialAssessmentResult is created only as part of successful submission`.

#### 17.4 ClarificationResult HTTP serialization is not frozen yet

Current clarification `result` is null because only PENDING exists. When CLARIFIED is implemented, DTO serialization and the stable decision-source vocabulary need dedicated Web tests before being frozen.

## 18. Checkpoint Decision

**Step 4 is accepted as complete.**

The deterministic core workflow now reaches either:

```text
COMPLETED
```

or:

```text
AWAITING_CLARIFICATION
```

The remaining work is a new workflow phase rather than unfinished deterministic scoring.

## 19. Recommended Next Phase

Preserve the existing roadmap:

```text
Step 5 — Restart / Start New
Step 6 — Assessment History / Detail hardening
Step 7 — Clarification / Tie-break
Step 8 — AI Integration
```

Before Step 7, resolve the persisted clarification read-model item described above.

## 20. Interview Explanation

> Questionnaire submission is modeled as one irreversible application transaction. The client sends the complete final snapshot, the backend validates it against the exact AssessmentDefinitionVersion bound to the Session, performs deterministic scoring, stores an immutable InitialAssessmentResult, evaluates ambiguity, and either finalizes immediately or creates durable pending clarification work. PostgreSQL constraints and integration tests protect the same lifecycle invariants at the persistence layer.

## 21. Suggested Git Commit

```text
feat(assessment): complete questionnaire submission and scoring
```

Suggested body:

```text
- add deterministic questionnaire scoring and ambiguity evaluation
- persist immutable initial and final assessment results
- make questionnaire submission an atomic business transaction
- create pending clarification lifecycle for ambiguous dimensions
- support immediate completion for clear results
- expose post-submission assessment session state
- add authoritative session detail read for recovery
- enforce submission conflict and incomplete-questionnaire errors
- add PostgreSQL and Web MVC coverage for submission workflow
- document the assessment submission and scoring checkpoint
```

Optional tag:

```text
assessment-submission-scoring-checkpoint
```
