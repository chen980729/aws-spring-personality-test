# Frontend Architecture Baseline

> **Original checkpoint:** Frontend F0
> **Status:** Accepted baseline; validated by implementation through Frontend F7-D and the post-F6 UI optimization checkpoint
> **Original date:** 2026-09-30
> **Implementation review:** 2026-10-04

## 1. Purpose

This document records the frontend architecture that was frozen before React implementation began. The implemented F1-F6 browser flow has validated the main behavioral boundaries below. A later presentation-layer refactor also validated that the UI can evolve independently without changing the backend-authoritative workflow, route model, or API integration rules. Implementation-specific outcomes are recorded in [`05-implementation-checkpoint-f1-f6.md`](05-implementation-checkpoint-f1-f6.md).

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

### 2.3 Version-bound Tie-break presentation and rollout compatibility

The frontend does not infer contextual option-to-pole mappings. For DefinitionVersion 1.1 it reads the Backend-owned contextual interaction and submits only the stable question/option IDs.

The retained 1.0 path is deliberately different during rollout: it continues to render its existing direct-pole choice from the Session's immutable questionnaire evidence and does not depend on the new GET interaction endpoint. This preserves compatibility while the CD workflow deploys Backend and Frontend in parallel and old ECS tasks may still be draining.

```text
1.0 Session -> existing direct-pole UI -> PUT selectedPole
1.1 Session -> GET contextual question -> PUT questionId + selectedOptionId
```

Unknown future DefinitionVersion codes are not guessed into either behavior; the current frontend shows an unsupported-contract recovery state instead.

### 2.4 Step 8 contract review note

The current workflow projection exposes:

```text
pendingClarificationDimensions
retryableClarificationDimensions
tieBreakRequiredDimensions
completed
```

It does not expose `activeClarificationDimension` explicitly. The current deterministic frontend infers the single active clarification from `clarifications[].status == IN_PROGRESS`; real provider-backed conversation is still deferred, so this is not a blocker for the completed F1-F6 scope.

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

The implemented Assessment feature follows the same ownership idea and currently contains:

```text
features/assessment/
├── api/
├── pages/
├── workflow/
├── questionnaire/
├── clarification/
├── tiebreak/
├── result/
└── history/
```

`workflow/` contains the presentation resolver (`AssessmentSessionResponse -> UI view`). It is deliberately not named `domain/`; the backend Domain remains the owner of lifecycle rules and valid transitions.

### 4.3 `shared/`

Only truly cross-feature infrastructure belongs here, initially:

```text
shared/
├── api/
├── ui/
└── lib/
```

`shared/api` owns HTTP/CSRF/Problem Details infrastructure. `shared/ui` and `shared/lib` should grow only when real reuse appears; they are not dumping grounds for business helpers.

The post-F6 UI refinement validated a small set of domain-independent primitives in `shared/ui`:

```text
Button
Card
FormField
PageHeader
```

These primitives expose presentation semantics only. They must not import Authentication, Assessment, Group, or other feature/domain concerns.

### 4.4 Presentation layer and styling boundary

The frontend uses a lightweight custom presentation layer rather than a general-purpose component framework for the current MVP. The boundary is intentionally small:

```text
app/styles/global.css
    semantic design tokens + global element baseline

app/styles/layouts.css
    public/authenticated application shell

shared/ui/*
    cross-feature presentation primitives

features/*
    feature/page composition and feature-specific visual states
```

Accepted rules:

- semantic CSS tokens define shared colors, spacing, radius, shadows, layout widths and focus treatment instead of scattering literal values across feature pages;
- `PublicLayout` and `AuthenticatedLayout` own application framing/navigation, while route pages own their content composition;
- feature CSS may style feature-specific workflow states but must not move business lifecycle decisions into the presentation layer;
- shared UI primitives remain domain-independent and are introduced only where reuse is demonstrated;
- responsive behavior, loading/error/empty states, focus treatment and reduced-motion support belong to presentation concerns and must not change API or workflow semantics;
- purely visual decoration should not change accessible names or labels relied on by users and tests.

A larger UI framework can be reconsidered if future Group/administrative surfaces materially increase component-system complexity. For the current portfolio MVP, the custom layer keeps dependency and abstraction cost proportional to the product scope.

### 4.2 Static presentation assets

Presentation-only imagery is owned by the frontend build and does not participate in Assessment Domain versioning.

Current examples:

```text
frontend/public/personality/*.webp
frontend/public/landing-personality-groups.webp
```

Result illustrations are mapped by stable personality type code. The Landing hero is a single optimized visual asset referenced directly by the public page.

These assets may change independently from `AssessmentDefinitionVersion` because they do not alter questionnaire, scoring, clarification or finalization semantics.

The Landing hero is treated as a first-screen performance asset: the component provides intrinsic width/height and requests eager/high-priority loading, while CSS owns responsive sizing.

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

## 7. Type/client generation rule

`docs/api/openapi.yaml` is a design-first MVP contract and intentionally includes future Group, deletion and provider-backed clarification paths that are not all executable today.

The implementation therefore does not generate and treat the entire OpenAPI document as a currently available runtime client.

Until executable backend and target OpenAPI are fully aligned, frontend TypeScript DTO/API modules remain scoped to implemented Controller/DTO/Security behavior. This rule prevented Group/deletion/provider-backed AI paths from becoming fake runtime dependencies. Full OpenAPI client generation can be reconsidered after the executable surface and design contract converge.

## 8. Implementation alignment through F6

The F0 architecture has now been exercised by the real frontend:

- `AuthenticatedLayout` protects Assessment/History routes using the current-user query;
- `/assessment-sessions/:sessionId` remains the only Session detail/workflow route;
- Questionnaire, Clarification, Tie-break and Result re-render from authoritative Session state without step-navigation routes;
- Restart navigates to a replacement Session resource with a new ID;
- History uses `/history?page=N`, while transient History-origin navigation is kept in Router state rather than added to the Session URL;
- Result presentation uses a frontend read model to explain backend decision provenance without recomputing Domain results;
- the post-F6 UI refactor introduced semantic design tokens, public/authenticated App Shells, reusable `shared/ui` primitives, responsive layouts and consistent loading/error/empty/404 presentation without changing route, API, state-ownership or workflow contracts;
- no empty Group feature, deletion client or live LLM interaction client exists before the corresponding executable backend boundary.

Historical Assessment deletion remains deferred until Group sharing exists because it is a cross-module orchestration, not an isolated frontend action.
