# REST API & HTTP Contract Design

> **Status:** Accepted REST Semantics — deterministic Clarification/Tie-break HTTP aligned through Assessment Step 7
> **Last updated:** 2026-09-30
> **Security transport:** server-side Session + Spring Session JDBC + Secure/HttpOnly cookie + CSRF

## 1. API style

MVP uses:

```text
REST-oriented JSON HTTP API
/api/v1 path versioning
RFC 9457 Problem Details errors
explicit lifecycle commands where business semantics matter
authenticated actor inferred from Security context
```

Resource reads are resource-oriented. Lifecycle transitions are exposed as explicit business commands where a generic status PATCH would weaken semantics.

No endpoint allows the client to set arbitrary Aggregate status.

## 2. Actor identity

Authenticated actor identity is never accepted from a client-controlled body field. Controller resolves current principal -> `UserId` -> Application command.

Authentication tells us *who*; the relevant Application layer decides *what that user may do*.

## 3. Content types

Normal JSON:

```text
application/json
```

Problem responses:

```text
application/problem+json
```

No-body success uses `204 No Content`.

## 4. Assessment endpoints

```text
GET    /api/v1/assessments
GET    /api/v1/assessments/{assessmentCode}

POST   /api/v1/assessments/{assessmentCode}/sessions
GET    /api/v1/assessments/{assessmentCode}/sessions/active

POST   /api/v1/assessment-sessions/{sessionId}/restart
GET    /api/v1/assessment-sessions
GET    /api/v1/assessment-sessions/{sessionId}
DELETE /api/v1/assessment-sessions/{sessionId}

GET    /api/v1/assessment-sessions/{sessionId}/questionnaire
PUT    /api/v1/assessment-sessions/{sessionId}/questionnaire
POST   /api/v1/assessment-sessions/{sessionId}/questionnaire/submission

POST   /api/v1/assessment-sessions/{sessionId}/clarifications/{dimensionCode}/start
POST   /api/v1/assessment-sessions/{sessionId}/clarifications/{dimensionCode}/messages
POST   /api/v1/assessment-sessions/{sessionId}/clarifications/{dimensionCode}/retry
POST   /api/v1/assessment-sessions/{sessionId}/clarifications/{dimensionCode}/skip
POST   /api/v1/assessment-sessions/{sessionId}/clarifications/skip-remaining

PUT    /api/v1/assessment-sessions/{sessionId}/tie-breaks/{dimensionCode}
```

### StartAssessment

`POST /assessments/{code}/sessions` implements create-if-none/resume-existing semantics.

- `201 Created` + Location when created.
- `200 OK` when existing active Session is returned.

### RestartAssessmentSession / Start New

`POST /assessment-sessions/{sessionId}/restart` expresses one complete business intent: replace **this identified active attempt** with a new attempt. The request has no body; actor identity comes from the authenticated principal and the path identifies the exact old Session.

Backend locks/revalidates the owned old Session, transitions it to `ABANDONED`, creates the replacement for the same `AssessmentDefinition`, binds the replacement to the unique current `AVAILABLE` `AssessmentDefinitionVersion`, and commits atomically.

Allowed old states are `IN_PROGRESS`, `AWAITING_CLARIFICATION`, and `CLARIFICATION_IN_PROGRESS`. `COMPLETED` and `ABANDONED` are terminal.

Success contract:

```text
201 Created
Location: /api/v1/assessment-sessions/{replacementSessionId}
```

Response body:

```json
{
  "abandonedSessionId": "old-session-id",
  "session": {
    "...": "AssessmentSessionResponse for the new IN_PROGRESS attempt"
  }
}
```

The replacement is a fresh Session with an empty questionnaire draft and `submitted = false`. The abandoned old Session keeps its own original DefinitionVersion binding and any already-persisted historical facts.

Restart conflicts are state-specific:

```text
old Session already ABANDONED
-> 409 ASSESSMENT_SESSION_ALREADY_ABANDONED

old Session already COMPLETED
-> 409 ASSESSMENT_SESSION_ALREADY_COMPLETED
```

A guessed/missing Session or a Session owned by another user returns `404 ASSESSMENT_SESSION_NOT_FOUND`.

If the first restart succeeds but its response is lost, retrying against the same old `sessionId` must not abandon the new successor. The retry returns `409 ASSESSMENT_SESSION_ALREADY_ABANDONED`; the client then calls `GET /assessments/{assessmentCode}/sessions/active` to recover the replacement.

A completed historical attempt is not restarted. “Take Again” after completion uses normal `StartAssessment`; when no active Session exists, Start creates a fresh attempt.

### Session-bound questionnaire

`GET /assessment-sessions/{sessionId}/questionnaire` returns the questionnaire rendering definition and the current response snapshot for the exact `AssessmentDefinitionVersion` bound to that Session. It must **not** resolve the current catalog `AVAILABLE` version when resuming an older Session.

The response exposes only public rendering data (`questionId`, order, prompt, answer-scale labels) plus the owner's saved response; scoring metadata such as dimension mapping, keyed pole, scoring policy, ambiguity policy and finalization policy remains server-side.

### Questionnaire draft

`PUT .../questionnaire` replaces the complete current questionnaire snapshot. The same snapshot can safely be retried.

### Questionnaire submission

`POST .../questionnaire/submission` carries the complete final snapshot again, preventing a last-autosave race. Submit plus deterministic scoring/ambiguity setup/immediate finalization is one atomic local transaction.

Re-submitting an already submitted Session is a business conflict, not a second scoring run. Client recovery contract:

```text
first Submit commits
-> HTTP response is lost
-> client retries Submit
-> 409 ASSESSMENT_ALREADY_SUBMITTED
-> client GETs /assessment-sessions/{sessionId}
-> client resumes from authoritative current state
```

### Clarification

The deterministic HTTP capabilities implemented in Step 7 are:

```text
POST .../clarifications/{dimensionCode}/skip
POST .../clarifications/skip-remaining
PUT  .../tie-breaks/{dimensionCode}
```

They return the authoritative current `AssessmentSessionResponse` after re-evaluating deterministic finalization readiness.

The design also reserves Start / Continue / Retry interaction routes. Those runtime assistant-output endpoints remain deferred until Step 8 wires the external LLM boundary; they must not be exposed as a fake half-implemented interaction surface merely because the Application execution boundary already exists internally.

When provider integration is enabled, technical AI failures become `FAILED_RETRYABLE`; they are never represented as the business result `UNCLEAR`. Retry reuses the same logical `Session + Dimension` lifecycle after ownership/state/eligibility revalidation and creates a new internal execution correlation token.

### Tie-break

`PUT .../tie-breaks/{dimensionCode}` sends only the selected pole. Backend validates that the bound dimension is an unresolved exact questionnaire tie and that the pole belongs to the bound `DimensionDefinition`.

The same already-persisted tie-break value is retry/recovery friendly. A different value may not rewrite an accepted historical decision.

### Finalization

There is deliberately no `/finalize` endpoint. Client submits facts; Domain decides when Session becomes `COMPLETED`.

### Assessment history

`GET /assessment-sessions?status=COMPLETED&page=1&size=20` lists only the authenticated owner's history. Detail uses the existing Session endpoint.

Historical deletion is:

```text
DELETE /api/v1/assessment-sessions/{sessionId}
```

The HTTP client need not know that implementation internally coordinates Group shares before hard deletion.

## 5. Group endpoints

```text
POST   /api/v1/groups
GET    /api/v1/groups
GET    /api/v1/groups/{groupId}
GET    /api/v1/groups/{groupId}/members
DELETE /api/v1/groups/{groupId}/members/me

PUT    /api/v1/groups/{groupId}/admin
POST   /api/v1/groups/{groupId}/disband

POST   /api/v1/group-join-requests
GET    /api/v1/group-join-requests
GET    /api/v1/groups/{groupId}/join-requests
POST   /api/v1/groups/{groupId}/join-requests/{joinRequestId}/approval
POST   /api/v1/groups/{groupId}/join-requests/{joinRequestId}/rejection
```

### Group creation

Client supplies only user-owned input such as `name`. Backend generates GroupCode and derives creator/Admin/current Membership from authentication/business rules.

### Join request

A user who only knows a GroupCode submits:

```text
POST /api/v1/group-join-requests
```

with GroupCode. The user cannot set `userId`, `status` or `expiresAt`.

### Approve / reject

Use explicit `/approval` and `/rejection` commands instead of PATCHing request status. This gives transaction/authorization/business meaning an explicit endpoint.

### Admin transfer

`PUT /groups/{groupId}/admin` accepts `newAdminMembershipId`, making “must be an ACTIVE member of this Group” explicit in the input model.

### Leave

`DELETE /groups/{groupId}/members/me` removes the *current active membership resource* at the HTTP boundary even though persistence keeps the historical Membership row as `ENDED`.

### Disband

Use explicit `POST .../disband`; Group is retained historically rather than hard-deleted.

## 6. Assessment-sharing endpoints

```text
POST   /api/v1/groups/{groupId}/members/me/assessment-share
PUT    /api/v1/groups/{groupId}/members/me/assessment-share
DELETE /api/v1/groups/{groupId}/members/me/assessment-share

GET    /api/v1/groups/{groupId}/shared-assessments
GET    /api/v1/groups/{groupId}/members/{membershipId}/shared-assessment
```

POST starts sharing, PUT replaces the currently shared result, DELETE stops consent and is retry-friendly/idempotent.

Group-facing result representations are intentionally narrower than owner-facing Assessment detail. They do not include questionnaire answers, private AI context, unshared history or unrelated results.

## 7. Identity & Security endpoints

```text
GET  /api/v1/auth/csrf
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/users/me
```

Authentication uses server-side Spring Security Session state persisted through Spring Session JDBC. User-facing Group read models use Identity-owned `displayName`; they do not expose email as a display identity. Production carries the opaque session identifier in a Secure/HttpOnly/SameSite=Lax `__Host-SESSION` cookie. Unsafe requests require `X-CSRF-TOKEN`, obtained from `GET /api/v1/auth/csrf`. Group Admin remains a Domain fact rather than a global Spring Security role. See `08-authentication-security.md`.

## 8. Pagination and sorting

Potentially unbounded collections use page/size parameters:

```text
page starts at 1
default size 20
maximum size 100
```

MVP uses page/offset pagination rather than cursor pagination.

Do not expose generic arbitrary sorting. Each use case defines stable business order, e.g. Assessment history by completed time descending, pending requests by requested time ascending.

## 9. Status-code policy

| HTTP | Usage |
|---|---|
| 200 | successful read/update/command with response body |
| 201 | resource created |
| 204 | successful action/delete with no body |
| 400 | malformed transport / request validation |
| 401 | missing/invalid authentication |
| 403 | visible resource but actor not authorized for action |
| 404 | resource absent or intentionally not visible |
| 409 | current resource/business state conflicts with requested action |
| 422 | syntactically valid input has invalid business semantics |
| 503 | temporary external dependency failure |
| 500 | unexpected server fault |

### 409 vs 422

409 examples:

- pending JoinRequest already exists.
- JoinRequest already resolved.
- Admin attempts Leave.
- Assessment already submitted.
- Restart targets an already-abandoned AssessmentSession.
- Restart targets a completed AssessmentSession.
- Group already disbanded.
- concurrent modification.

422 examples:

- questionnaire incomplete.
- invalid answer value.
- invalid dimension/pole for the definition.
- tie-break not semantically acceptable.

### 403 vs 404

Private resources such as another user's guessed AssessmentSession return 404 to avoid confirming existence. A member who can see a Group but attempts an Admin-only action receives 403.

## 10. Problem Details

Use RFC 9457 Problem Details plus stable extensions:

```json
{
  "type": "/problems/join-request-already-pending",
  "title": "Join request already pending",
  "status": 409,
  "detail": "A pending join request already exists for this group.",
  "instance": "/api/v1/group-join-requests",
  "code": "JOIN_REQUEST_ALREADY_PENDING",
  "traceId": "..."
}
```

Transport validation may add `fieldErrors`. Cooldown errors may add `retryAt`.

Frontend branches on stable `code`, not human-readable `detail` strings.

Never return stack traces, SQL, internal class names or provider secrets.

## 11. Representative stable error codes

```text
GROUP_NOT_FOUND
GROUP_DISBANDED
ACTIVE_MEMBERSHIP_ALREADY_EXISTS
ADMIN_CANNOT_LEAVE
JOIN_REQUEST_ALREADY_PENDING
JOIN_REQUEST_COOLDOWN_ACTIVE
JOIN_REQUEST_ALREADY_RESOLVED
JOIN_REQUEST_EXPIRED
ASSESSMENT_SESSION_NOT_FOUND
ASSESSMENT_SESSION_ALREADY_ABANDONED
ASSESSMENT_SESSION_ALREADY_COMPLETED
ASSESSMENT_SESSION_CONCURRENT_MODIFICATION
ASSESSMENT_ALREADY_SUBMITTED
QUESTIONNAIRE_INCOMPLETE
INVALID_QUESTIONNAIRE_RESPONSE
CLARIFICATION_NOT_ALLOWED
CLARIFICATION_NOT_RETRYABLE
CLARIFICATION_ALREADY_IN_PROGRESS
TIE_BREAK_NOT_REQUIRED
INVALID_DIMENSION_TIE_BREAK
AI_CLARIFICATION_TEMPORARILY_UNAVAILABLE
```

Java exception class names are not the API contract.

## 12. DTO boundary

Web request/response DTOs are not Domain objects and are not JPA entities.

Conceptual direction:

```text
HTTP DTO
 -> Application command/query input
 -> Domain

Domain/query execution
 -> Application result/read model
 -> HTTP response DTO
```

Avoid pointless duplication where ownership does not differ, but never serialize Domain/JPA objects directly.

## 13. Internal optimistic version

The JPA `@Version` value is not exposed as an HTTP contract in MVP. ETag / `If-Match` is deferred until there is a real client-side concurrent-editing requirement.

## 14. Idempotency

Naturally retry-friendly operations include complete-snapshot PUTs and DELETE of current active resources.

`StartAssessment` uses POST but has create-if-none/resume semantics. Explicit Restart is retry-recoverable because it targets the old Session being replaced rather than “whatever Session is active now”. A successful Restart returns `201 Created` with a Location for the replacement; retrying the old target returns a stable terminal-state conflict and the client recovers via the active-Session read. Submit retry uses the stable conflict + GET recovery protocol described above.

MVP does not introduce generic `Idempotency-Key` infrastructure. If future operations require stronger duplicate-request replay semantics, add it deliberately.

## 15. Exact DTO Contract / OpenAPI

The machine-readable contract lives at:

```text
docs/api/openapi.yaml
OpenAPI Specification: 3.1.2
```

The design-first machine-readable contract is published as OpenAPI **v0.4.0**. The Step 7 update aligns deterministic Clarification/Tie-break HTTP behavior, including the `422` business-semantic response for `skip-remaining`. Runtime Start / Continue / Retry clarification routes remain design-frozen for Step 8 provider integration rather than being represented as already-implemented runtime AI behavior.

The Step 5 Restart and Step 6 History contracts remain unchanged; the History item matches the implemented response exactly:

```text
AssessmentHistoryItem
  - sessionId
  - assessmentCode
  - assessmentVersion
  - finalType
  - completedAt
```

The earlier v0.3.0 bump captured the implemented History schema correction. The v0.4.0 bump records the Step 7 deterministic Clarification/Tie-break alignment rather than silently changing the published design contract.

It freezes:

- every path and method.
- request schemas and required/nullable fields.
- response schemas and stable envelopes.
- enum values.
- pagination.
- RFC 9457 Problem Details extensions.
- Session-cookie security scheme and CSRF header contract.
- concrete owner-vs-shared Assessment representations.

A deliberate privacy/anti-gaming boundary is that `AssessmentDetailsResponse` exposes only public questionnaire rendering data (`questionId`, order, prompt and answer scale). It does not expose `dimension`, `keyedPole`, scoring/ambiguity rules or other DefinitionVersion internals.

OpenAPI documents the chosen Domain/Application/API design; it must not redefine those boundaries.
