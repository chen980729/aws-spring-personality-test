# Frontend API Integration Baseline

> **Original checkpoint:** Frontend F0
> **Status:** Accepted baseline; implemented through Frontend F6 for the executable Identity + deterministic Assessment surface
> **Original date:** 2026-09-30
> **Implementation review:** 2026-10-03

## 1. Contract sources

The repository currently has both a design-first target contract and an incrementally implemented runtime.

For frontend implementation, distinguish:

```text
requirements / architecture
        ↓
design-first OpenAPI target
        ↓
currently executable backend
```

The executable frontend baseline is determined by the implemented Spring Controllers, DTOs, Security configuration and accepted backend checkpoint behavior.

`docs/api/openapi.yaml` remains the target HTTP contract, but paths for future Group, historical deletion and live LLM interaction are not evidence that those endpoints are executable.

## 2. Currently executable API surface

### 2.1 Identity / authentication

| Method | Path | Authentication | CSRF |
|---|---|---:|---:|
| GET | `/api/v1/auth/csrf` | Public | No |
| POST | `/api/v1/auth/register` | Public | Yes |
| POST | `/api/v1/auth/login` | Public | Yes |
| POST | `/api/v1/auth/logout` | Authenticated lifecycle endpoint | Yes |
| GET | `/api/v1/users/me` | Required | No |

Registration creates an account but does not automatically authenticate it.

### 2.2 Assessment catalog / entry

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/assessments` | available Assessment catalog |
| GET | `/api/v1/assessments/{assessmentCode}` | Assessment introduction/detail |
| GET | `/api/v1/assessments/{assessmentCode}/sessions/active` | locate current active Session |
| POST | `/api/v1/assessments/{assessmentCode}/sessions` | start or return existing active Session |

### 2.3 Session / questionnaire

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/assessment-sessions/{sessionId}` | authoritative Session state |
| POST | `/api/v1/assessment-sessions/{sessionId}/restart` | atomically abandon and replace active Session |
| GET | `/api/v1/assessment-sessions/{sessionId}/questionnaire` | exact-version questionnaire + saved response |
| PUT | `/api/v1/assessment-sessions/{sessionId}/questionnaire` | replace current saved questionnaire snapshot |
| POST | `/api/v1/assessment-sessions/{sessionId}/questionnaire/submission` | immutable submit boundary |

Questionnaire autosave is a complete snapshot replacement contract, not incremental per-question PATCH semantics.

### 2.4 Deterministic Clarification / Tie-break boundary

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/assessment-sessions/{sessionId}/clarifications/{dimensionCode}/skip` | skip one unresolved clarification |
| POST | `/api/v1/assessment-sessions/{sessionId}/clarifications/skip-remaining` | skip remaining unresolved clarifications |
| PUT | `/api/v1/assessment-sessions/{sessionId}/tie-breaks/{dimensionCode}` | submit required exact-tie user choice |

These operations return the updated authoritative `AssessmentSessionResponse`.

### 2.5 History

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/assessment-sessions?status=COMPLETED&page=1&size=20` | completed history |
| GET | `/api/v1/assessment-sessions/{sessionId}` | current or historical Session detail |

## 3. Design target but not executable yet

Do not build current runtime dependencies on:

- provider-backed clarification Start / Message / Retry endpoints;
- historical Assessment deletion;
- Group / membership / sharing endpoints.

The frontend architecture should leave extension points for them without mocking them as already available product behavior.

## 4. Query ownership

Recommended query-key families:

```text
["auth", "me"]
["assessments"]
["assessments", assessmentCode]
["assessments", assessmentCode, "active-session"]
["assessment-session", sessionId]
["assessment-session", sessionId, "questionnaire"]
["assessment-history", page, size]
```

The helper names have evolved during implementation, but ownership remains feature-local and query keys still separate current-user, catalog/detail, active Session, Session/questionnaire and History state.

## 5. Authoritative Session mutation rule

For Assessment mutations that return `AssessmentSessionResponse`:

```text
mutation
  ↓
backend commits authoritative state
  ↓
response
  ↓
set/update Session query cache
  ↓
canonical Session route re-renders
```

Do not maintain a separate client workflow step counter.

## 6. Questionnaire two-layer state

Saved questionnaire state is server state. User edits that have not yet been saved are local draft state.

```text
server saved snapshot
        ↓
initialize frontend draft
        ↓
user edits
        ↓
debounced/scheduled PUT complete snapshot
        ↓
success updates saved snapshot
```

Do not use `localStorage` as a second questionnaire persistence system. Backend autosave remains the persistence source of truth.

The implemented Questionnaire flow resolves those concurrency concerns as follows:

```text
local draft changes
    -> debounce
    -> serialize complete-snapshot PUTs (one in flight)
    -> if edits occur while saving, immediately persist the latest snapshot after the current save

Submit
    -> stop scheduling new autosaves
    -> clear pending debounce
    -> wait for any already in-flight autosave
    -> POST the latest complete local snapshot
```

A failed autosave keeps the latest local answers and explicit Retry saves the current draft rather than replaying an older failed payload. `ASSESSMENT_ALREADY_SUBMITTED` is treated as an uncertain-outcome recovery signal and triggers an authoritative Session GET rather than re-running scoring.

## 7. Problem Details contract

Frontend behavior must branch on the stable machine-readable Problem Details `code`, not on human-readable `detail` text.

Useful frontend categories are:

### Protocol / cross-cutting

```text
AUTHENTICATION_REQUIRED
CSRF_VALIDATION_FAILED
ACCESS_DENIED
```

### User-correctable input/business request

Examples:

```text
VALIDATION_FAILED
QUESTIONNAIRE_INCOMPLETE
INVALID_QUESTIONNAIRE_RESPONSE
INVALID_DIMENSION_TIE_BREAK
```

### Authoritative-state recovery

Examples:

```text
ASSESSMENT_ALREADY_SUBMITTED
ASSESSMENT_SESSION_CONCURRENT_MODIFICATION
CLARIFICATION_NOT_ALLOWED
TIE_BREAK_NOT_REQUIRED
```

State-recovery codes should generally trigger a reload/refetch of authoritative Session state rather than blind replay of stale client assumptions.

## 8. Retry policy

Generic mutation retry is disabled by default.

A state-changing request may have reached and committed on the backend even when the browser did not receive the response. Irreversible/state-sensitive commands therefore use feature-specific recovery semantics.

Example:

```text
Submit succeeds
response is lost
user retries
  ↓
ASSESSMENT_ALREADY_SUBMITTED
  ↓
GET authoritative AssessmentSession
  ↓
recover Clarification/Result UI
```

Read queries use limited retry for network/transient failures: the current QueryClient does not retry deterministic `4xx` API responses and retries network/`5xx` read failures at most once. Mutations do not use generic automatic retry.

## 9. Current frontend integration alignment

The F1-F6 implementation consumes the executable endpoints above through handwritten feature-local TypeScript DTO/API modules and the shared HTTP client.

Important implemented rules:

- unsafe requests obtain CSRF state centrally;
- `CSRF_VALIDATION_FAILED` refreshes CSRF and replays the request exactly once;
- final `AUTHENTICATION_REQUIRED` transitions the current-user cache to anonymous;
- Session mutations cache the authoritative returned `AssessmentSessionResponse` and let the canonical Session route resolve the next presentation;
- completed Session mutations invalidate Assessment History;
- History is currently `COMPLETED`-only because that is the executable backend query contract;
- provider-backed clarification, historical deletion and Group paths remain intentionally unused even though they are present in the design-first target.

Real-browser integration found contract/lifecycle issues that MSW alone could not prove, including the wrapped Restart response shape and a backend exact-tie response-mapping bug. This is why the executable Spring runtime remains the final integration authority even when frontend mocks are contract-shaped.
