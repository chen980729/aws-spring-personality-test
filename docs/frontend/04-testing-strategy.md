# Frontend Testing Strategy

> **Original checkpoint:** Frontend F0
> **Status:** Accepted; Vitest/RTL/MSW strategy implemented through Frontend F7-B and exercised through the UI optimization checkpoint
> **Original date:** 2026-09-30
> **Implementation review:** 2026-10-04

## 1. Goal

Frontend tests should complement, not duplicate, the existing backend Domain/Application/Web MVC/PostgreSQL test suite.

The frontend must primarily prove:

- a backend-authoritative state is rendered correctly;
- a user action sends the expected HTTP intent;
- loading/error/recovery behavior is correct;
- browser-only Session/CSRF/navigation/persistence integration actually works;
- presentation refactors preserve accessible interaction contracts instead of only preserving visual appearance.

It should not re-prove backend Domain invariants in JavaScript.

## 2. Test layers

| Layer | Tool | Main question | Real backend? |
|---|---|---|---:|
| Pure unit | Vitest | Does a small frontend function/state helper behave correctly? | No |
| Component / frontend integration | Vitest + React Testing Library + MSW | Does the frontend consume the HTTP contract and behave correctly from the user's perspective? | No |
| Browser E2E | Playwright | Does the real Browser -> React -> Spring Security/Application -> PostgreSQL chain work? | Yes |
| Backend suite | JUnit / MockMvc / Testcontainers | Are backend business/security/persistence rules correct? | Backend only |

## 3. Unit tests

Good unit-test targets include:

- `resolveAssessmentSessionView`;
- `sanitizeReturnTo`;
- Problem-code classification;
- query-key builders;
- small questionnaire draft/save helpers;
- `CsrfTokenManager` concurrency/invalidation behavior.

Unit tests do not need React, Router, MSW or Spring Boot.

## 4. Component / integration tests with RTL + MSW

This is the primary frontend test layer.

Prefer testing user-observable behavior through accessible queries such as role, label and visible content.

MSW should intercept HTTP at the network boundary rather than mocking feature API modules directly.

Preferred path:

```text
Component/Page
    ↓
real query/mutation hook
    ↓
real feature API module
    ↓
real shared HTTP client
    ↓
fetch
    ↓
MSW
```

This exercises the frontend integration layers while replacing only the backend.

Representative scenarios:

- Login success and invalid credentials;
- protected-route authentication state;
- Session response maps to Questionnaire/Clarification/Tie-break/Result;
- Skip returns a completed Session and UI becomes Result without manual step navigation;
- Problem Details `code` produces the intended recovery UI;
- autosave loading/success/failure/stale-order behavior;
- lost-response submit recovery through `ASSESSMENT_ALREADY_SUBMITTED` + Session refetch.

## 5. Playwright E2E boundary

Playwright is reserved for behavior that benefits materially from a real browser and real backend:

```text
Browser
  ↓
React/Vite or built frontend
  ↓
Spring Boot
  ↓
Spring Security / Spring Session / CSRF
  ↓
PostgreSQL
```

High-value first vertical-slice scenarios:

1. Register -> Login -> `/me` -> browser refresh -> Logout.
2. Start Assessment -> answer -> autosave -> browser refresh -> resume saved state.
3. Submit a deterministic non-ambiguous result -> Result.
4. Ambiguous result -> Skip -> required Tie-break when applicable -> Result.
5. Start New -> replacement Session ID and abandoned old Session behavior.
6. Complete -> History -> reopen completed Session/Result.
7. Session/CSRF expiry during an unsafe action -> CSRF recovery -> authentication recovery.

E2E should validate system boundaries, not exhaust every business error branch already covered by backend tests and MSW-based frontend tests.

## 6. E2E data/environment

Playwright must use a dedicated test PostgreSQL environment, not a developer's normal local data.

A future CI/local E2E topology may be:

```text
Playwright
   ↓
Frontend
   ↓
Spring Boot e2e profile
   ↓
Dedicated PostgreSQL test instance
```

Tests should establish normal business setup through real HTTP APIs where practical. Direct database insertion should be avoided unless later performance needs justify carefully scoped test fixtures.

`auth.spec.ts` should exercise the real Registration/Login UI. Other E2E specs may establish authenticated browser state through real authentication HTTP requests/helpers rather than repeating the same UI flow in every test.

## 7. Test organization

Small unit/component tests are co-located with implementation files.

Shared frontend-test infrastructure belongs under:

```text
src/test/
├── setup.ts
├── render.tsx
└── msw/
    └── server.ts
```

Assessment-specific fixtures/handlers remain with the Assessment feature rather than becoming a global fixture dump.

Playwright specs live separately:

```text
e2e/
├── auth.spec.ts
├── assessment-questionnaire.spec.ts
├── assessment-workflow.spec.ts
└── history.spec.ts
```

## 8. What not to optimize for

- Do not make snapshot testing the default testing strategy.
- Do not test React Router, TanStack Query or React Hook Form internals themselves.
- Do not target mechanical 100% code coverage as an MVP goal.
- Do not repeat every backend Domain invariant in TypeScript.

Coverage should be risk-driven. Highest priority goes to authentication state, CSRF recovery, workflow presentation, autosave concurrency, irreversible-command recovery and Problem Details branching.


## 9. Current implementation status

The automated frontend suite currently uses the first two frontend layers from this strategy:

```text
Vitest pure-unit tests
React Testing Library + MSW component/integration tests
TypeScript production build
ESLint
```

Implemented regression coverage now includes CSRF recovery, auth/session restoration, Restart response handling, invalid Session IDs, serialized questionnaire autosave, Submit/autosave coordination, `ASSESSMENT_ALREADY_SUBMITTED` recovery, React StrictMode effect replay, Clarification/Tie-break workflow resolution, Result provenance, History pagination and History -> canonical Session -> Result navigation. F7-A added contextual Tie-break GET parsing, contextual PUT request shape + CSRF, and `TIE_BREAK_QUESTION` result-source presentation. F7-B adds component/workflow coverage for contextual prompt/options, confirmation, completed-result transition and the rolling-deploy compatibility rule: legacy 1.0 tests intentionally provide no GET tie-break handler, so any accidental dependency on the new endpoint fails as an unhandled MSW request.

The implementation produced three testing lessons worth keeping explicit:

1. **Passing Vitest does not replace `npm run build` or `npm run lint`.** Vitest's transform path can execute tests even when TypeScript project compilation or React Hooks lint rules would reject the implementation.
2. **MSW can faithfully reproduce a wrong frontend assumption.** The Restart wrapper mismatch passed frontend tests because the mock and TypeScript assertion agreed with each other; real-browser integration against Spring Boot exposed the contract mismatch.
3. **Accessible queries are useful presentation-regression contracts.** During the UI refactor, placing a visual required marker inside the `<label>` DOM changed the label text observed by Testing Library and broke six auth tests before any business behavior ran. The fix kept the required marker visual through CSS while restoring stable accessible label text. The tests were preserved rather than weakened to match accidental markup.

The project has repeatedly performed manual full-stack browser verification against Vite + Spring Boot + PostgreSQL. Dedicated Playwright E2E remains deferred until the Cloud/CI pipeline provides a stable repeatable full-stack environment. When added, it should remain small and boundary-focused rather than duplicate the existing Java and MSW suites.
