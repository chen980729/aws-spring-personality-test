# Assessment Restart / Start New Checkpoint

> **Checkpoint:** Assessment Step 5 complete  
> **Date:** 2026-09-27  
> **Status:** Accepted after automated concurrency/integration/Web MVC testing

## 1. Milestone

Step 5 completes the explicit **Restart / Start New** capability for an unfinished active Assessment attempt.

The important distinction is:

```text
StartAssessment
= create if none / otherwise resume existing active Session

RestartAssessmentSession
= explicitly abandon this identified active Session
  and atomically create a replacement
```

Normal Start never destroys current progress.

## 2. Accepted business semantics

Restart is allowed only from:

```text
IN_PROGRESS
AWAITING_CLARIFICATION
CLARIFICATION_IN_PROGRESS
```

Restart is not allowed from:

```text
COMPLETED
ABANDONED
```

A successful Restart produces two durable facts:

```text
old Session
-> ABANDONED

new Session
-> IN_PROGRESS
```

The replacement has:

```text
new AssessmentSessionId
same owner
same AssessmentDefinition
current unique AVAILABLE AssessmentDefinitionVersion
empty QuestionnaireResponse
not submitted
```

The old Session keeps its original bound version and already-persisted attempt facts.

## 3. Version semantics

Resume and Restart deliberately behave differently:

```text
Resume
-> same Session
-> same exact bound DefinitionVersion

Restart / Start New
-> new Session
-> same AssessmentDefinition
-> current AVAILABLE DefinitionVersion
```

This preserves historical reproducibility while allowing a newly started attempt to use the currently executable assessment specification.

## 4. Atomicity

Restart is one Assessment Application transaction:

```text
lock exact owned old Session
-> revalidate active state
-> resolve current AVAILABLE version
-> old Session -> ABANDONED
-> create replacement Session
-> preserve one-active-session invariant
-> commit
```

The committed result must never be:

```text
old = ABANDONED
replacement = missing
```

If replacement creation fails, the entire transaction rolls back.

## 5. Concurrency model

Restart uses pessimistic coordination for the old Session because the command performs a state-dependent cross-Session replacement.

The protected race is:

```text
Restart request A(old)
Restart request B(old)
```

Expected result:

```text
A locks old
A abandons old
A creates successor
A commits

B then reloads/revalidates old
B sees ABANDONED
B conflicts
```

The durable one-active-session invariant remains enforced by the database partial UNIQUE index.

This complements, rather than replaces, optimistic locking used for Autosave. The two use cases have different concurrency requirements.

## 6. Persistence ordering

The implementation mixes managed JPA updates for the old Session with the existing atomic JDBC insert path for active Session creation.

Therefore the Infrastructure adapter must ensure that the old active Session transition is flushed before attempting the replacement insert so the database uniqueness rule observes the old attempt as non-active.

This is an Infrastructure responsibility, not a Domain/Application concept.

The Domain Repository contract should describe **atomic replacement semantics**, not JPA `flush()` or PostgreSQL mechanics.

## 7. Retry recovery

The API intentionally targets the old Session:

```text
POST /api/v1/assessment-sessions/{oldSessionId}/restart
```

It does not use a command such as:

```text
POST /assessments/{code}/restart-current
```

Reason:

```text
first Restart:
A -> ABANDONED
B -> IN_PROGRESS

response lost

retry against A:
A is already ABANDONED
-> stable conflict
-> B remains untouched
```

A “restart current” endpoint could accidentally treat B as the new target and create C on retry.

## 8. HTTP contract

Successful Restart:

```text
201 Created
Location: /api/v1/assessment-sessions/{replacementSessionId}
```

Body:

```text
RestartAssessmentResponse
├── abandonedSessionId
└── session: AssessmentSessionResponse
```

Important errors:

```text
401 AUTHENTICATION_REQUIRED
403 CSRF_VALIDATION_FAILED
404 ASSESSMENT_SESSION_NOT_FOUND
409 ASSESSMENT_SESSION_ALREADY_ABANDONED
409 ASSESSMENT_SESSION_ALREADY_COMPLETED
```

Another user's guessed Session remains privacy-safe 404.

## 9. Completed history vs Start New

A `COMPLETED` Session is immutable historical data and is not converted to `ABANDONED`.

If the user wants to take the Assessment again after completion:

```text
normal StartAssessment
-> no active Session exists
-> create a new Session
```

The Restart command is specifically for abandoning an unfinished active attempt.

## 10. Test coverage

Step 5 is accepted after repeated passing tests across the intended layers.

### Application

Verified:

```text
IN_PROGRESS restart
AWAITING_CLARIFICATION restart
replacement uses current AVAILABLE version
already-abandoned old Session rejected
completed Session rejected
missing / other-owner Session rejected
```

### PostgreSQL Testcontainers

Verified:

```text
old Session becomes ABANDONED
replacement becomes the only active Session
replacement starts with empty draft
partial UNIQUE invariant remains valid
JPA-update / JDBC-insert ordering works
```

### Concurrency

Repeatedly verified:

```text
two concurrent restarts against same old Session
-> exactly one success
-> exactly one abandoned-state conflict
-> exactly one active replacement
```

### Web MVC / Security

Verified:

```text
201 + Location
response contains old/new Session identity
404 private/not-found behavior
409 already abandoned
409 already completed
403 missing CSRF
401 unauthenticated
```

## 11. Review findings

### Accepted

The implementation is consistent with the frozen Domain/Application/API design:

```text
Start != Restart
explicit old Session targeting
one local transaction
same Definition / current AVAILABLE Version
terminal-state protection
database-backed one-active invariant
retry recovery
focused concurrency testing
```

### Small cleanup recommendation

Keep Infrastructure details out of the Domain Repository abstraction.

A repository method such as:

```text
replaceActive(old, replacement)
```

is a reasonable Domain-facing persistence capability because it expresses the atomic invariant.

However, comments in the Domain package should not prescribe:

```text
EntityManager.flush()
JdbcTemplate
PostgreSQL partial index implementation details
```

Those details belong in the Infrastructure adapter and its tests.

This is a boundary-cleanliness improvement, not a Step 5 blocker.

### Contract synchronization item

The REST design document is updated in this checkpoint. The standalone machine-readable `docs/api/openapi.yaml` was not available as an editable source in the supplied materials for this review.

Before the next published API-contract version, synchronize it with:

```text
Restart 201 + Location
RestartAssessmentResponse
ASSESSMENT_SESSION_ALREADY_COMPLETED
Restart 404 / 409 responses
```

Do not claim a new OpenAPI contract version until that file is actually updated and validated.

## 12. Next phase

Step 6 should focus on:

```text
Assessment History
+
historical Session detail hardening
```

The important new concerns are now read-side concerns:

```text
owner scoping
completed-only history
completedAt DESC
pagination
read projections
exact bound DefinitionVersion
historical privacy
```

This is a good point to avoid adding more mutation complexity until the historical read model is complete.

## 13. Suggested commit

```text
feat(assessment): complete restart and start-new flow
```

Suggested body:

```text
- add explicit restart/start-new application flow
- lock and revalidate the old active session
- atomically abandon the old attempt and create a replacement
- bind replacement attempts to the current available definition version
- preserve one-active-session invariant under concurrency
- add retry-safe restart HTTP contract with 201 Location
- add completed/abandoned restart conflicts
- add PostgreSQL concurrency and Web MVC coverage
- align assessment domain, application, REST and testing docs
```
