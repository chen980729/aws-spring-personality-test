# Frontend Documentation

> **Current checkpoint:** Frontend F7-E complete; DefinitionVersion 1.1 compatibility is deployed and activation requires no further frontend code
> **Last updated:** 2026-10-06
> **Project next focus:** provider-backed LLM clarification interaction; Group/Sharing follows when its backend capability becomes executable
> **Deferred frontend work:** provider-backed LLM interaction, Group UI, and historical Assessment deletion after the required backend capabilities exist

This directory contains the accepted frontend architecture baseline plus the implementation checkpoint for the currently executable browser application.

The frontend is not treated as a second source of business truth. It expresses user intent, renders backend-authoritative state, and provides browser-level integration across Session/CSRF, routing, recovery, autosave and Assessment workflow behavior.

## Documents

- [`01-frontend-architecture.md`](01-frontend-architecture.md) — route/page boundaries, feature ownership, project structure, Assessment workflow rendering model and presentation-layer boundary.
- [`02-api-integration.md`](02-api-integration.md) — executable API baseline, target-vs-runtime contract distinction, server-state ownership, mutation/recovery rules and Problem Details usage.
- [`03-auth-session-csrf.md`](03-auth-session-csrf.md) — browser Session/CSRF protocol, same-origin development topology, login/logout rotation behavior and authentication recovery.
- [`04-testing-strategy.md`](04-testing-strategy.md) — Vitest/RTL/MSW/Playwright responsibilities, cross-stack browser testing boundaries and presentation-regression lessons.
- [`05-implementation-checkpoint-f1-f6.md`](05-implementation-checkpoint-f1-f6.md) — implemented React stack, accepted F1-F6 behavior, post-F6 UI optimization checkpoint, integration findings, remaining boundaries and cloud handoff.
- [`06-result-content-checkpoint-f7c.md`](06-result-content-checkpoint-f7c.md) — F7-C content-version boundary, 8-letter / 16-type catalogs, result imagery, UI hierarchy, tests and acceptance checkpoint.
- [`07-landing-hero-checkpoint-f7d.md`](07-landing-hero-checkpoint-f7d.md) — F7-D Landing hero asset integration, responsive/LCP/accessibility decisions and acceptance checkpoint.
- [`08-final-review-checkpoint-f7e.md`](08-final-review-checkpoint-f7e.md) — F7-E final regression/documentation/release-readiness review, merge strategy and staged activation checklist.

## Current implementation summary

```text
Register / Login / Session / Logout
        ↓
Assessment Catalog / Detail
        ↓
Start / Resume / Start New
        ↓
Questionnaire / Autosave / Refresh Recovery
        ↓
Submit
        ↓
Backend-authoritative Workflow Resolver
        ↓
Clarification Read / Skip / Skip Remaining
        ↓
Exact Tie-break when required
        ↓
Version-aware Tie-break contract
(DIRECT_POLE_SELECTION / CONTEXTUAL_QUESTION)
        ↓
Result Read Model / Decision Provenance
        ↓
Completed History / Canonical Session Detail
```

The implemented browser application uses:

```text
React + TypeScript + Vite
React Router
TanStack Query
React Hook Form
Vitest + React Testing Library + MSW
```

Server state remains in TanStack Query, unsaved questionnaire edits remain local React state, and Session/CSRF protocol state stays inside the shared HTTP boundary.

The completed presentation layer now uses:

```text
semantic CSS design tokens
PublicLayout + AuthenticatedLayout App Shells
shared/ui: Button / Card / FormField / PageHeader
feature-specific page/workflow composition
responsive + loading/error/empty/404 polish
```

This UI layer is intentionally lightweight and custom for the current MVP; no general-purpose component framework is required at the present scope. The visual refactor did not change feature API contracts or backend-authoritative workflow rules.

## Current boundaries

The current executable frontend deliberately does **not** pretend that design-first future API paths already exist.

Still deferred:

- provider-backed Start / Continue / Retry clarification interaction (Backend Step 8);
- Group / Membership / Sharing UI until the Group backend is executable;
- historical Assessment deletion until Group sharing exists, because deletion is a cross-domain operation that must end active shares before hard-deleting the Assessment Session;
- dedicated Playwright E2E automation; the implemented flows are covered by Vitest/RTL/MSW and have also been exercised manually against the real Spring Boot + PostgreSQL stack.

Cloud/Delivery is established and the automatic CD path is verified. DefinitionVersion 1.1 frontend compatibility is implemented through F7-B, F7-C adds the presentation content layer required to make completed results understandable, and F7-D replaces the former CSS-generated Landing visual with the optimized four-profile hero artwork. The hero is responsive, uses intrinsic dimensions and high-priority eager loading for the first-screen image, and preserves the existing headline/CTA/features content. F7-E final regression/documentation/release review is complete, V8 activation is deployed, and new 1.1 Sessions use the already-compatible contextual Tie-break UI without further frontend changes.
