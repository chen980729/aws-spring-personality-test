# Assessment Session Read Checkpoint

## 1. Checkpoint

**Assessment Catalog + Session Start/Resume + Read Path Complete**

This checkpoint closes the Assessment read path before the project moves into questionnaire mutation, submission, scoring, clarification, and restart workflows.

The checkpoint should be treated as complete only after the automated tests and manual verification listed in this document pass.

---

## 2. Scope

The backend supports the following authenticated flow:

```text
Assessment Catalog
    ↓
Start / Resume AssessmentSession
    ↓
Get Active AssessmentSession
    ↓
Get Session-bound Questionnaire
```

Implemented endpoints in this checkpoint:

```text
GET  /api/v1/assessments
GET  /api/v1/assessments/{assessmentCode}

POST /api/v1/assessments/{assessmentCode}/sessions
GET  /api/v1/assessments/{assessmentCode}/sessions/active

GET  /api/v1/assessment-sessions/{sessionId}/questionnaire
```

---

## 3. Assessment Definition and Version Model

`AssessmentDefinition` represents the long-lived identity of an assessment type.

`AssessmentDefinitionVersion` represents one immutable executable version of that assessment.

For the current MVP:

```text
AssessmentDefinition:
SIXTEEN_PERSONALITY

AssessmentDefinitionVersion:
1.0
```

Definition/version data is application-owned reference data created through Flyway migrations rather than runtime CRUD operations.

The executable assessment specification is stored in PostgreSQL JSONB and restored through explicit persistence JSON representations and mappers.

---

## 4. Session Version Binding

An `AssessmentSession` permanently binds:

```text
ownerUserId
definitionId
definitionVersionId
```

These values do not change during the lifetime of the Session.

An existing Session must continue using the exact `AssessmentDefinitionVersion` with which it was started, even if the catalog later publishes a newer AVAILABLE version.

Example:

```text
User starts Session
→ DefinitionVersion 1.0

Later:
1.0 → RETIRED
1.1 → AVAILABLE

User resumes existing Session
→ still uses 1.0
```

Existing Session reads therefore resolve:

```text
session.definitionVersionId
    ↓
AssessmentDefinitionVersionRepository.findById(...)
```

They must not resolve:

```text
AssessmentDefinitionVersionRepository.findAvailableByDefinitionId(...)
```

This rule is protected by automated tests.

---

## 5. Start / Resume Semantics

Starting an Assessment uses create-if-none / resume-existing semantics:

```text
No active Session
→ create new IN_PROGRESS Session
→ HTTP 201 Created

Active Session already exists
→ return existing Session
→ HTTP 200 OK
```

Normal Start never silently abandons existing progress.

Restart / Start New is a separate business command and is intentionally outside this checkpoint.

---

## 6. One Active Session Invariant

For one:

```text
User + AssessmentDefinition
```

there may be at most one active `AssessmentSession`.

Active statuses are:

```text
IN_PROGRESS
AWAITING_CLARIFICATION
CLARIFICATION_IN_PROGRESS
```

The invariant is protected at two levels.

Application normal path:

```text
find active Session
→ resume if present
```

Database concurrency protection:

```text
PostgreSQL partial UNIQUE index
+
INSERT ... ON CONFLICT DO NOTHING
```

Concurrent Start requests therefore result in exactly one persisted active Session.

The losing request reloads and returns the authoritative winner instead of returning its own unpersisted candidate.

---

## 7. Questionnaire Response Foundation

`AssessmentSession` owns an immutable `QuestionnaireResponse` snapshot:

```text
AssessmentSession
└── QuestionnaireResponse
    └── Answer[]
```

A new Session starts with:

```text
QuestionnaireResponse.empty()
```

rather than a null Domain value.

Before questionnaire submission, the Session may replace its current snapshot with another immutable `QuestionnaireResponse`.

After submission, response replacement is not allowed.

`QuestionnaireResponse` currently enforces structural invariants such as one answer per `QuestionId`.

Definition-version-driven semantic validation, including valid question IDs and valid answer values, is intentionally deferred to `SaveQuestionnaireProgress`.

---

## 8. Owner-scoped Session Reads

Private Session resources use owner-scoped persistence queries.

Conceptually:

```sql
WHERE id = :sessionId
  AND user_id = :actorUserId
```

The Application receives the same result for:

```text
Session does not exist

and

Session exists but belongs to another user
```

Both are exposed as:

```text
404
ASSESSMENT_SESSION_NOT_FOUND
```

This avoids confirming the existence of another user's private AssessmentSession.

---

## 9. Active Session Read

The active Session endpoint is:

```text
GET /api/v1/assessments/{assessmentCode}/sessions/active
```

It returns the current active Session owned by the authenticated user for the requested AssessmentDefinition.

The response uses the Session's bound `definitionVersionId`; it does not re-resolve the current AVAILABLE catalog version.

Saved questionnaire answers are included in the Session read model so Start/Resume and Get Active do not lose draft state once autosave is introduced.

---

## 10. Session-bound Questionnaire Read

The questionnaire endpoint is:

```text
GET /api/v1/assessment-sessions/{sessionId}/questionnaire
```

It combines:

```text
owned AssessmentSession
+
exact bound AssessmentDefinitionVersion
+
current QuestionnaireResponse snapshot
```

The public response contains only rendering information and the owner's response state:

```text
assessment code/version
questionId
position
prompt
answer scale
saved answers
submitted state
```

It intentionally does not expose server-side scoring metadata:

```text
dimension
keyedPole
scoringPolicy
ambiguityPolicy
clarificationPolicy
finalizationPolicy
```

This is both an API boundary and an anti-gaming boundary.

---

## 11. Application Read Model

`AssessmentSessionResult` is the shared Application projection used by Session-facing operations such as:

```text
Start / Resume
Get Active Session
```

This prevents Start/Resume from losing saved questionnaire answers after autosave is introduced.

Web DTOs remain separate from:

```text
Domain objects
Application results
JPA entities
Persistence JSON models
```

---

## 12. Persistence Design

Read-oriented Spring Data repositories expose only the operations required by the Domain repository adapters rather than extending full `JpaRepository` CRUD APIs.

Race-safe Session creation uses a small PostgreSQL-specific JDBC component:

```text
PostgresAssessmentSessionAtomicCreator
```

Its responsibility is limited to the concurrency-sensitive operation:

```sql
INSERT ... ON CONFLICT ... DO NOTHING
```

Ordinary entity reads remain implemented through Spring Data / JPA.

This is an intentional hybrid persistence design:

```text
normal reads
→ JPA / Spring Data

PostgreSQL-specific atomic create
→ JdbcTemplate
```

---

## 13. Test Coverage

This checkpoint should include automated coverage for:

```text
Assessment specification invariants

Definition / DefinitionVersion persistence

Assessment catalog queries

Catalog HTTP anti-gaming boundary

AssessmentSession lifecycle

QuestionnaireResponse structural invariants

PostgreSQL race-safe active Session creation

Start / Resume application semantics

201 Created vs 200 Resume HTTP semantics

Authentication and CSRF behavior

Owner-scoped Session lookup

Get Active AssessmentSession

Exact bound DefinitionVersion loading

Session-bound questionnaire rendering

Saved questionnaire response projection

Questionnaire scoring-metadata exclusion

ASSESSMENT_SESSION_NOT_FOUND privacy behavior
```

A particularly important regression test verifies that Session reads never call:

```text
findAvailableByDefinitionId(...)
```

for an already-bound Session.

---

## 14. Manual Verification

After the complete automated suite passes:

```bash
./mvnw test
```

run the application:

```bash
./mvnw spring-boot:run
```

Then verify the authenticated flow.

### Start / Resume

First request:

```text
POST /api/v1/assessments/SIXTEEN_PERSONALITY/sessions
```

Expected:

```text
201 Created
created = true
```

Repeat the same request:

```text
POST /api/v1/assessments/SIXTEEN_PERSONALITY/sessions
```

Expected:

```text
200 OK
created = false
same sessionId
```

### Get Active Session

```text
GET /api/v1/assessments/SIXTEEN_PERSONALITY/sessions/active
```

Expected:

```text
200 OK
same sessionId
assessment.version = bound version
status = IN_PROGRESS
questionnaire.answers = []
```

for a newly-created Session.

### Get Session Questionnaire

```text
GET /api/v1/assessment-sessions/{sessionId}/questionnaire
```

Expected:

```text
200 OK
48 questions for SIXTEEN_PERSONALITY 1.0
response.answers = []
response.submitted = false
```

The response must not contain:

```text
dimension
keyedPole
scoringPolicy
ambiguityPolicy
clarificationPolicy
finalizationPolicy
```

---

## 15. Explicitly Deferred

This checkpoint does not include:

```text
PUT questionnaire autosave

Definition-version-driven answer validation

Domain → JSONB response persistence

Optimistic-lock update conflict handling

Questionnaire submission

Deterministic scoring

InitialAssessmentResult

AI clarification

Tie-breaks

FinalAssessmentResult

Restart / Start New

Assessment history
```

These are intentionally deferred rather than partially implemented.

---

## 16. Next Step

The next implementation phase is:

```text
Assessment Step 3B
Save Questionnaire Progress / Autosave
```

Target endpoint:

```text
PUT /api/v1/assessment-sessions/{sessionId}/questionnaire
```

The next phase will introduce:

```text
complete snapshot replacement
DefinitionVersion-driven answer validation
Domain → JSONB persistence mapping
AssessmentSession persistence update
@Version optimistic locking
retry-friendly PUT semantics
```

This marks the transition from the Assessment read path to the mutable Assessment workflow.

---

## 17. Suggested Git Commit

```text
feat(assessment): complete session read path
```

Suggested body:

```text
- add shared assessment session application read model
- expose authenticated active session lookup
- expose session-bound questionnaire rendering
- preserve exact definition version binding on resume
- include persisted questionnaire response snapshots
- enforce owner-scoped private session reads
- return privacy-safe session not-found responses
- protect questionnaire APIs from scoring metadata leakage
- add assessment session read checkpoint documentation
```
