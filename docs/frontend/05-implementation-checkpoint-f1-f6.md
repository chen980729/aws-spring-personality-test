# Frontend F1-F6 Implementation Checkpoint

> **Status:** Accepted
> **Date:** 2026-10-04
> **Scope:** executable Authentication + deterministic Assessment browser flow through Result/History, plus post-F6 presentation-layer optimization
> **Next project workstream:** AWS deployment / containerization / Terraform / CI/CD

## 1. Checkpoint result

The original F0 architecture baseline has now been exercised through a real React implementation.

Implemented browser flow:

```text
Register / Login / Session / CSRF
        ↓
Assessment Catalog / Detail
        ↓
Start / Resume / Restart
        ↓
Questionnaire / Autosave / Refresh Recovery
        ↓
Submit
        ↓
Clarification Read Model / Skip
        ↓
Tie-break when required
        ↓
Deterministic Finalization
        ↓
Result / History / Canonical Session Detail
```

The frontend remains intentionally incomplete for capabilities whose executable backend boundaries do not yet exist: real LLM interaction, Group/Sharing, and cross-domain historical Assessment deletion.

## 2. F1 — React + TypeScript foundation

The root `frontend/` build unit now uses:

```text
React 19
TypeScript
Vite
React Router
TanStack Query
React Hook Form
Vitest + jsdom
React Testing Library
MSW
ESLint
```

Accepted structure:

```text
src/
├── app/        application composition, providers, routes/layouts
├── features/   auth + assessment business-facing UI
├── shared/     HTTP/CSRF/error infrastructure + reusable presentation primitives
└── test/       shared test setup/render/MSW server
```

Development uses the Vite `/api` and `/actuator` proxy to Spring Boot so feature code always uses relative URLs.

## 3. F2 — Authentication / Session / CSRF

Implemented:

- Register and Login forms with React Hook Form;
- current-user server state through `GET /api/v1/users/me`;
- authenticated route boundary;
- safe internal `returnTo` handling;
- Logout;
- in-memory `CsrfTokenManager`;
- unsafe-request CSRF header injection;
- one-time `CSRF_VALIDATION_FAILED` refresh/retry;
- final `AUTHENTICATION_REQUIRED` propagation into the current-user cache;
- Session restoration after browser refresh.

A real-browser integration issue exposed that Spring Session derives a persisted principal name through `Authentication#getName()`. A record-style principal without `AuthenticatedPrincipal#getName()` could serialize a long `toString()` value and exceed Spring Session's `PRINCIPAL_NAME` column. The backend principal now implements `AuthenticatedPrincipal` and returns the immutable `userId` as its name. The fix preserves the standard Spring Session schema instead of widening it to accommodate accidental principal formatting.

## 4. F3 — Assessment entry and canonical Session routing

Implemented:

- Assessment catalog and detail;
- active Session lookup;
- Start / Resume;
- explicit Start New confirmation;
- Restart response handling and navigation to the replacement Session ID;
- `/assessment-sessions/:sessionId` as the canonical Session route;
- invalid UUID rejection before HTTP;
- deterministic `4xx` read failures excluded from generic TanStack Query retry.

A real-browser test caught an incorrect frontend assumption about Restart: the backend returns `RestartAssessmentResponse { abandonedSessionId, session }`, not a bare Session. MSW had mirrored the wrong frontend assumption, demonstrating that a frontend mock can be internally consistent and still disagree with the real backend contract.

## 5. F4 — Questionnaire lifecycle

Implemented:

- Session-bound questionnaire definition retrieval;
- persisted-answer restoration;
- local unsaved draft ownership separate from TanStack Query server state;
- debounced complete-snapshot autosave;
- visible `Saved / Unsaved changes / Saving / Autosave failed` state;
- serialized autosave so at most one save is in flight;
- latest-draft retry after save failure;
- final Submit carrying its own complete snapshot;
- coordination that pauses new autosaves and waits for an already in-flight save before irreversible Submit;
- `ASSESSMENT_ALREADY_SUBMITTED` lost-response recovery by fetching authoritative Session state.

React development `StrictMode` exposed a lifecycle bug where effect cleanup left the autosave hook paused after the development setup/cleanup/setup replay. The hook now restores its active state during effect setup, and dedicated StrictMode regression tests cover both autosave and submission.

## 6. F5 — Deterministic Clarification and Tie-break workflow

Implemented without pretending that the real LLM runtime exists:

- typed Clarification read model;
- pure `AssessmentSessionResponse -> presentation view` workflow resolver;
- recovery UI when Session/workflow projections are internally inconsistent;
- Skip Current confirmation and mutation;
- Skip Remaining confirmation and mutation;
- authoritative cache update followed by resolver-driven next view;
- exact-tie Tie-break UI;
- tie-break options derived from backend dimension evidence rather than duplicated personality rules;
- explicit Review -> Confirm interaction;
- sequential multiple tie-break dimensions;
- deterministic transition to final Result.

The workflow resolver interprets backend state; it does not implement the Assessment Domain state machine in TypeScript.

Two integration corrections were important:

1. `TieBreakStage` is keyed by `dimensionCode` so local radio/confirmation state does not leak from one dimension into the next when React reuses the same component type.
2. A completed exact-tie Session originally failed during backend Web DTO mapping because a Java Stream mapped `questionnairePreference == null` to a null element before `findFirst()`. The transaction could already be committed, causing the mutation and later reads to return `500` even though the Session was completed. The mapper now resolves the element first and maps the nullable preference through `Optional`, with regression coverage.

## 7. F6 — Result and History read side

Implemented:

- Result Read Model combining immutable initial evidence with final decision provenance;
- explicit distinction between `no-baseline`, `matches-questionnaire`, and `overrode-questionnaire`;
- typed final-decision source presentation;
- consistency checks between initial and final questionnaire preference projections;
- completed Assessment History list;
- backend 1-based pagination represented in `/history?page=N`;
- empty/error states;
- History cache invalidation when a Session becomes `COMPLETED`;
- History records linking to the canonical `/assessment-sessions/:sessionId` route;
- transient Router state preserving the exact History return page without polluting the canonical Session URL;
- authenticated primary navigation for Assessments / History.

The frontend Result layer explains backend decision provenance; it never recomputes the personality result from questionnaire evidence.

## 8. Post-F6 UI optimization checkpoint

After the functional F1-F6 slice stabilized, the presentation layer was refined as a separate milestone rather than mixing visual redesign with business-flow implementation.

Implemented presentation changes:

- semantic design tokens for color, spacing, radius, shadow, layout width and focus treatment;
- dedicated `PublicLayout` and `AuthenticatedLayout` App Shell presentation;
- domain-independent `Button`, `Card`, `FormField` and `PageHeader` primitives under `shared/ui`;
- redesigned Landing, Login and Register surfaces;
- dashboard-style Assessment Catalog and completed History presentation;
- unified visual workflow for Detail -> Questionnaire -> Clarification -> Tie-break -> Result;
- responsive behavior for narrow/mobile layouts;
- consistent loading, error and empty states plus a styled 404 surface;
- hover/focus polish and `prefers-reduced-motion` handling.

The refactor deliberately did **not** change:

```text
API contracts
TanStack Query ownership
Assessment workflow resolver semantics
route identity/navigation rules
Authentication/CSRF protocol behavior
backend-authoritative business decisions
```

No external UI component framework was introduced. For the current product surface, a small custom design layer provides enough reuse while avoiding a larger dependency/abstraction surface. This choice can be revisited if later Group/administrative features materially expand UI-system complexity.

One regression during this milestone reinforced the accessibility/testing boundary: a required-field `*` rendered as DOM text inside `<label>` changed the accessible label text and caused six Login/Register tests using `getByLabelText` to fail. The marker was moved to CSS presentation, preserving both the visual cue and the semantic label contract.

## 9. State and navigation boundaries after implementation

The F0 ownership model remains valid:

| State | Current owner |
|---|---|
| current user, catalog, Session, questionnaire saved snapshot, history | TanStack Query |
| unsaved questionnaire answers, confirmations, local interaction state | React / React Hook Form |
| routes, History page, authentication `returnTo` | Router / URL |
| History-origin back target | transient Router state |
| CSRF token | in-memory shared HTTP infrastructure |

No assessment or authentication state is persisted to `localStorage`.

## 10. Testing checkpoint

Implemented automated frontend layers:

```text
Vitest pure unit tests
React Testing Library component/integration tests
MSW HTTP-boundary tests
TypeScript production build
ESLint
```

Risk-focused coverage includes:

- CSRF token caching/refresh and expired-session recovery;
- Register/Login/Logout/current-user restoration;
- safe `returnTo` handling;
- Start/Resume/Restart contract behavior;
- questionnaire draft ownership and autosave serialization;
- submission/autosave race handling;
- React StrictMode autosave lifecycle;
- workflow resolver consistency;
- Skip/Tie-break transitions from authoritative responses;
- Result provenance;
- History pagination and History -> canonical Session -> Result -> History navigation.

The same flows were repeatedly exercised manually against the real Browser -> Vite -> Spring Security/Application -> PostgreSQL stack during implementation. Dedicated Playwright E2E automation remains deferred and can be added when CI/CD creates a stable repeatable full-stack environment.

## 11. Deferred frontend boundaries

### Provider-backed clarification

The deterministic shell is implemented, but Start / Continue / Retry real AI interaction remains Backend Step 8. Before implementing that UI, review whether `AssessmentSessionResponse.workflow` should expose an explicit active clarification dimension rather than requiring inference from `clarifications[].status == IN_PROGRESS`.

### Group

No empty frontend Group feature is created before executable Group backend capability exists.

### Historical Assessment deletion

Deletion remains part of Core MVP but is intentionally deferred. It is not a standalone Assessment command in implementation terms: deleting a completed Session must first end every ACTIVE `GroupAssessmentShare` that references it with reason `ASSESSMENT_DELETED`, then hard-delete the Assessment Session in the same cross-module use case. Implementing deletion before Group sharing exists would either invent a fake dependency or force later rework.

## 12. Handoff to Cloud / Delivery work

Frontend feature development and the current-scope UI optimization are intentionally paused at this checkpoint. The next active project workstream is to make the already runnable vertical slice deployable and repeatable through:

```text
Docker application packaging
AWS deployment architecture
Terraform
GitHub Actions CI/CD
production configuration / secrets / cookie transport
runtime deployment verification
```

Provider-backed LLM work is intentionally postponed until after the Cloud/CI/CD line is established. Historical deletion resumes after the Group backend/sharing boundary exists.
