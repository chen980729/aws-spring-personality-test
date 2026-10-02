# Frontend Architecture Baseline

> **Checkpoint:** Frontend F0
> **Status:** Accepted
> **Date:** 2026-09-30

## 1. Purpose

This document freezes the frontend architecture before React implementation begins.

The primary goals are:

- keep backend lifecycle/business rules authoritative;
- make refresh, deep-link and browser recovery predictable;
- keep feature ownership clear as Assessment, AI clarification and Group evolve;
- avoid adding client-state infrastructure that the current product does not need;
- make the browser an early integration surface for Session, CSRF and REST behavior.

## 2. Architecture principles

### 2.1 Stable resources belong in routes

Routes identify stable resources or navigation intent. Transient Assessment workflow steps do not become independent route state.

Accepted route tree for the first vertical slice:

```text
/
├── /login
├── /register
├── /assessments
├── /assessments/:assessmentCode
├── /assessment-sessions/:sessionId
└── /history
```

`/assessment-sessions/:sessionId` is the canonical frontend URL for an Assessment Session.

Questionnaire, Clarification, Tie-break and Result are workflow views rendered inside that route. They are not separate routes such as `/questionnaire`, `/clarification` or `/result`.

### 2.2 Backend workflow state is authoritative

The frontend does not reproduce the Assessment state machine.

```text
GET AssessmentSession
        ↓
AssessmentSessionResponse
        ↓
small presentation resolver
        ↓
Questionnaire / Clarification / Tie-break / Result
```

The presentation resolver maps an authoritative backend response to a UI view. It does not decide whether a business transition is valid.

A useful presentation precedence for the current executable workflow is:

1. completed -> Result;
2. abandoned -> Abandoned Session state;
3. questionnaire not submitted -> Questionnaire;
4. unresolved clarification work -> Clarification;
5. required exact-tie decisions -> Tie-break;
6. otherwise -> explicit recovery/invalid-server-state UI rather than guessing.

The backend may expose both clarification work and required tie-break dimensions at the same time. The frontend presents clarification work first, then tie-breaks, to keep UX sequential without changing backend rules.

### 2.3 Step 8 contract review note

The current workflow projection exposes:

```text
pendingClarificationDimensions
retryableClarificationDimensions
tieBreakRequiredDimensions
completed
```

It does not expose `activeClarificationDimension` explicitly. The current Step 7 public runtime does not start real AI conversations, so this is not a blocker for F1-F8.

Before implementing provider-backed Step 8 clarification UI, review whether the public workflow projection should explicitly expose the active/in-progress dimension rather than requiring the frontend to infer it from `clarifications[].status == IN_PROGRESS`.

## 3. Page boundaries

Route-level pages:

```text
LandingPage
LoginPage
RegisterPage
AssessmentCatalogPage
AssessmentDetailPage
AssessmentSessionPage
AssessmentHistoryPage
NotFoundPage
```

Workflow views owned by `AssessmentSessionPage`:

```text
QuestionnaireView
ClarificationView
TieBreakView
ResultView
AbandonedSessionView
```

### 3.1 Assessment Detail: Start, Resume and Start New

`AssessmentDetailPage` should express the user's intent rather than hide existing lifecycle choices behind one generic button.

It may use:

```text
GET /api/v1/assessments/{assessmentCode}
GET /api/v1/assessments/{assessmentCode}/sessions/active
```

When no active Session exists:

```text
Start Assessment
  -> POST /api/v1/assessments/{assessmentCode}/sessions
  -> navigate to /assessment-sessions/{newId}
```

When an active Session exists:

```text
Resume
  -> navigate to /assessment-sessions/{activeId}

Start New
  -> confirmation UI
  -> POST /api/v1/assessment-sessions/{activeId}/restart
  -> navigate with replace to /assessment-sessions/{replacementId}
```

`POST .../sessions` remains retry-safe and may return an existing active Session, but the Detail Page should use the explicit active-session query when it needs to present Resume vs Start New UX.

### 3.2 History detail reuses the canonical Session route

There is no separate `/history/:sessionId` route.

`/history` lists completed sessions. Selecting one navigates to `/assessment-sessions/:sessionId`; a completed backend response naturally renders `ResultView`.

Pagination is navigation state and may use query parameters, for example:

```text
/history?page=2
```

## 4. Project structure

The frontend is a separate build unit at repository root:

```text
frontend/
├── src/
│   ├── app/
│   ├── features/
│   ├── shared/
│   └── test/
└── e2e/
```

The intended dependency direction is:

```text
app -> features -> shared
app -----------> shared
```

`shared` must not depend on `features` or `app`.

### 4.1 `app/`

Application composition only:

- Router;
- TanStack Query provider;
- global application shell;
- public/authenticated layouts;
- global loading/error boundaries where appropriate.

`app/` should not own feature API calls or business rules.

### 4.2 `features/`

Initial executable business features:

```text
features/
├── auth/
└── assessment/
```

History remains inside Assessment because it is the read side of the same `AssessmentSession` resource and historical detail reuses the canonical Session DTO/route.

Do not create an empty `group/` feature until Group has executable backend capability.

A likely Assessment shape is:

```text
features/assessment/
├── api/
├── pages/
├── session/
├── questionnaire/
├── clarification/
├── tie-break/
├── result/
└── test/
```

`session/` contains presentation mapping such as `resolveAssessmentSessionView`. It is deliberately not named `domain/`; backend Domain remains the owner of lifecycle rules.

### 4.3 `shared/`

Only truly cross-feature infrastructure belongs here, initially:

```text
shared/
├── api/
├── ui/
└── lib/
```

`shared/api` owns HTTP/CSRF/Problem Details infrastructure. `shared/ui` and `shared/lib` should grow only when real reuse appears; they are not dumping grounds for business helpers.

## 5. State ownership

Frontend state is classified by meaning rather than by one global store:

| State | Examples | Owner |
|---|---|---|
| Server State | current user, catalog, Session, questionnaire saved snapshot, history | TanStack Query |
| Draft/UI State | form inputs, unsaved questionnaire answers, modal/open state | React / React Hook Form |
| Navigation State | route, history page, safe `returnTo` | URL / Router |
| Protocol State | CSRF token | in-memory HTTP infrastructure |

Redux is not part of the F0 baseline because the current complexity is predominantly server state plus local form/draft state.

TanStack Query cache remains in memory; assessment/authentication data is not persisted into `localStorage`.

## 6. Mutation and navigation rule

When a mutation returns a new authoritative `AssessmentSessionResponse`, update the Session query cache and let the same canonical route re-render.

Examples:

```text
Submit
Skip
Skip Remaining
Tie-break
```

These do not navigate to separate workflow routes.

`Restart` is different because it creates a replacement resource with a new Session ID; navigation should move to the replacement ID, preferably with browser-history replacement so Back does not immediately return to the abandoned Session.

## 7. Type/client generation rule during F1

`docs/api/openapi.yaml` is a design-first MVP contract and intentionally includes future Group, deletion and provider-backed clarification paths that are not all executable today.

Therefore F1 must not generate and treat the entire OpenAPI document as a currently available runtime client.

Until executable backend and target OpenAPI are fully aligned, frontend TypeScript DTO/API modules should be scoped to implemented Controller/DTO/Security behavior. Full OpenAPI client generation can be reconsidered after the executable surface and design contract converge.
