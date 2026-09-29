# Backend Application Use Case Boundary

> **Status:** Accepted MVP Detailed Design — implementation-aligned through Assessment Step 7
> **Last updated:** 2026-09-30

## 1. Application-layer responsibility

Domain owns business invariants and legal state transitions. Application owns the business use case around them:

```text
load participating state
identify actor
perform business authorization
coordinate Aggregates
call Domain behavior
call outbound/module ports
persist
own transaction boundary
```

The project uses command/query separation as a responsibility convention, **not** full CQRS infrastructure. There is no Command Bus, Query Bus, separate read database or CQRS framework in MVP.

## 2. Application package style

Conceptually:

```text
assessment.application
├── command
├── query
└── port.out
```

Important mutation use cases are strongly use-case-oriented: e.g. `SubmitQuestionnaireService`, `ApproveJoinRequestService` rather than one huge `AssessmentService` or `GroupService`.

Queries may be more pragmatic and may share a read-oriented service when that improves simplicity.

## 3. Actor identity and authorization

Authenticated user identity is converted to an explicit `actorUserId` at the Web/Application boundary. Domain objects never access Spring Security context.

```text
Authentication: who is the caller?
Business authorization: may this caller perform this use case?
```

Business authorization belongs to Assessment/Group Application logic, not to a centralized Identity module.

## 4. Identity use cases

Commands:

- Register user.
- Authenticate user.
- Logout user.

Queries:

- Get current user.

Token/session mechanics remain deferred to Security Design.

## 5. Assessment command map

### StartAssessment

Semantics:

```text
no active Session -> create
active Session exists -> return/resume it
```

It never silently abandons current progress.

### RestartAssessmentSession (Start New)

Explicit **Start New** is a separate command from normal Start/Resume. The command targets the exact active `sessionId` being replaced; it does not mean “restart whichever Session is active now”.

Allowed source states:

```text
IN_PROGRESS
AWAITING_CLARIFICATION
CLARIFICATION_IN_PROGRESS
```

One Assessment Application transaction:

```text
lock identified owned Session for update
-> revalidate that this exact Session is still active
-> resolve the same AssessmentDefinition
-> resolve that Definition's unique current AVAILABLE DefinitionVersion
-> old Session -> ABANDONED
-> create replacement Session -> IN_PROGRESS
-> persist replacement while preserving the one-active-session invariant
-> commit
```

The replacement is a **new attempt** with a new `AssessmentSessionId`. It keeps the same `AssessmentDefinition`, but intentionally binds the unique current `AVAILABLE` `AssessmentDefinitionVersion`; it does not inherit the old Session's bound version merely because the old attempt used it. The abandoned Session retains its original version binding and historical facts.

`COMPLETED` and `ABANDONED` are terminal and cannot be restarted through this command. A user who wants to take an Assessment again after a completed historical attempt uses normal `StartAssessment`; because no active Session exists, Start creates a new attempt.

Concurrency/retry semantics are part of the use-case contract:

```text
two concurrent Restart requests against the same old sessionId
-> at most one replacement is created
-> the other request observes the old Session as ABANDONED and conflicts
```

If the first restart commits but its HTTP response is lost, retrying against the same already-abandoned old `sessionId` must not abandon the newly created replacement Session. The client recovers by reading the current active Session.

If an LLM call for the old Session is still in flight, its later result is stale and is discarded when the post-call transaction reloads/revalidates the now-abandoned Session.

Persistence ordering needed to satisfy the durable one-active-session constraint is an Infrastructure concern. The Application contract requires the old attempt and its replacement to be committed atomically; it does not expose JPA flush or PostgreSQL-specific mechanics.

### SaveQuestionnaireProgress

Accepts the entire current questionnaire snapshot rather than a single answer patch. `QuestionnaireResponse` remains an immutable value-object snapshot.

### SubmitQuestionnaire

Submission carries the complete final questionnaire snapshot so correctness does not depend on the timing of the last autosave.

One local transaction performs:

1. ownership/state validation.
2. final snapshot replacement.
3. submission fact persistence.
4. deterministic scoring.
5. immutable `InitialAssessmentResult` creation.
6. ambiguity evaluation.
7. clarification lifecycle creation where required.
8. immediate finalization when clarification is unnecessary.

This transaction is atomic in MVP. If it does not commit, submission did not happen. If it commits, there is no persisted `submitted-but-not-scored` intermediate state.

If the HTTP success response is lost and the client submits again, the second command returns the stable already-submitted conflict and does not score again. Client recovery is `GetAssessmentSession(sessionId)`.

There is no public `FinalizeAssessment` command. Finalization is a Domain consequence after sufficient facts exist.

### Clarification workflow

Commands:

- Begin clarification.
- Continue clarification.
- Retry current clarification.
- Skip one clarification.
- Skip remaining clarifications.
- Submit dimension tie-break.

Only ambiguous dimensions may enter clarification. At most one dimension may be `IN_PROGRESS` per Session. `DimensionClarification` is a separate Assessment Aggregate Root (ADR-0016), while `AssessmentSession` remains the concurrency/finalization anchor.

Clarification mutations therefore lock/revalidate the Session, load the target Clarification Aggregate, and persist the required Session + Clarification changes in one short local transaction. PostgreSQL uniqueness protects one lifecycle per Session/dimension and at most one `IN_PROGRESS` Clarification per Session.

Technical AI failures become `FAILED_RETRYABLE`; they must never be reinterpreted as the business result `UNCLEAR`. `RetryDimensionClarification` transitions the same logical Clarification Aggregate back into active processing after revalidation; retry never creates a second `DimensionClarification` for the same Session + Dimension.

### External LLM transaction rule

Never wait for an external LLM call inside a long database transaction.

Use:

```text
short DB transaction A
-> validate / enter IN_PROGRESS
-> generate + persist a new opaque active execution token
-> return an internal execution ticket carrying that token
-> commit

external LLM call
(no DB transaction / row lock)

short DB transaction B
-> reload / revalidate Session + Clarification
-> require the ticket token to match the current active execution token
-> accept result/failure only for the current execution
-> discard stale completion when Session/state/token changed
-> possibly finalize
-> commit
```

## 6. Assessment queries

- List available assessments.
- Get assessment details for the currently `AVAILABLE` version.
- Get active assessment.
- Get AssessmentSession.
- **Get Session-bound Questionnaire** — load rendering data from the exact `AssessmentDefinitionVersion` already bound to the Session; never substitute the current `AVAILABLE` version when resuming an older Session.
- List Assessment history.
- Get Assessment history detail.

Owner-facing read models may expose the user's own questionnaire/result evidence according to product needs; shared Group representations are separate and narrower.

## 7. Group command map

Pure Group commands:

- Create Group.
- Request to join Group.
- Approve JoinRequest.
- Reject JoinRequest.
- Transfer Admin.
- Leave Group.
- Disband Group.
- Stop sharing Assessment result.
- Expire pending JoinRequests (system lifecycle operation).

Examples of consistency boundaries:

```text
CreateGroup
= create Group + creator ACTIVE Membership + creator Admin

ApproveJoinRequest
= approve request + create Membership

LeaveGroup
= end Membership + end its active Share

DisbandGroup
= disband Group + end Memberships + reject pending requests + end Shares
```

These are coordinated by Group Application rather than by expanding Group into one enormous Aggregate.

## 8. Group queries

Pure Group queries include:

- Get Group.
- Get my Groups.
- Get Group members.
- Get pending JoinRequests.
- Get my JoinRequests.

Queries that combine Group visibility and Assessment result data belong to cross-module orchestration.

## 9. Cross-module orchestration commands

### StartSharingAssessmentResult

1. Assessment validates Session exists, belongs to actor and is `COMPLETED`.
2. Group validates actor's Membership and Group state.
3. Group creates active consent/share record.
4. Commit atomically.

### ChangeSharedAssessmentResult

Validate the replacement Assessment first, then end the old Share with `RESULT_REPLACED` and create the new active Share atomically.

### DeleteHistoricalAssessment

1. lock/validate owned deletable AssessmentSession.
2. Group ends all active Shares referencing it with `ASSESSMENT_DELETED`.
3. Assessment hard-deletes the historical Session and its Session-dependent persisted data.
4. commit atomically.

## 10. Cross-module orchestration queries

- Get Group shared results.
- Get one member's shared result.

Group decides visibility and resolves the active Share; Assessment returns a narrow `ShareableAssessmentResult`.

## 11. Transaction ownership

| Use case category | Transaction owner |
|---|---|
| Identity mutation | Identity Application |
| Assessment-only mutation | Assessment Application |
| Group-only mutation | Group Application |
| Cross Assessment/Group mutation | Orchestration |
| Query | normally read-only transaction where useful |

Default Spring transaction propagation remains `REQUIRED`; no Saga, distributed transaction or `REQUIRES_NEW` architecture is introduced for the MVP.

## 12. Business-authorization examples

- Assessment mutation/read detail: actor must own the Session.
- Approve/reject/transfer/disband: actor must be current Admin.
- Leave: actor owns Membership and must not currently be Admin.
- Sharing: actor owns active Membership and completed Assessment result.
- Shared-result read: viewer must have valid Group visibility.
- Delete Assessment: actor must own a historical deletable Session.

Group Admin does **not** become an Assessment administrator.

## 13. Command vs Query persistence

Commands operate through Domain Aggregates and Domain Repository abstractions to protect invariants. A use case may coordinate more than one Aggregate when the workflow requires it; for example, Clarification commands coordinate `AssessmentSession` and `DimensionClarification` under the Session lock instead of pretending both live in one in-memory Aggregate.

Queries may use direct optimized projections when no Domain mutation is occurring. Example: `GetMyGroups` may read a projection rather than hydrating full Group and Membership Aggregates.

## 14. Core rule

```text
One business intent -> one clear application boundary.
Domain decides what is legal.
Application decides how participants are coordinated.
```
