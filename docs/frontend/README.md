# Frontend Documentation

> **Current checkpoint:** Frontend F6 complete — deterministic Assessment read/write flow + Result/History integration
> **Last updated:** 2026-10-03
> **Project next focus:** AWS deployment, containerization, Terraform and CI/CD
> **Deferred frontend work:** provider-backed LLM interaction, Group UI, and historical Assessment deletion after the required backend capabilities exist

This directory contains the accepted frontend architecture baseline plus the implementation checkpoint for the currently executable browser application.

The frontend is not treated as a second source of business truth. It expresses user intent, renders backend-authoritative state, and provides browser-level integration across Session/CSRF, routing, recovery, autosave and Assessment workflow behavior.

## Documents

- [`01-frontend-architecture.md`](01-frontend-architecture.md) — route/page boundaries, feature ownership, project structure and Assessment workflow rendering model.
- [`02-api-integration.md`](02-api-integration.md) — executable API baseline, target-vs-runtime contract distinction, server-state ownership, mutation/recovery rules and Problem Details usage.
- [`03-auth-session-csrf.md`](03-auth-session-csrf.md) — browser Session/CSRF protocol, same-origin development topology, login/logout rotation behavior and authentication recovery.
- [`04-testing-strategy.md`](04-testing-strategy.md) — Vitest/RTL/MSW/Playwright responsibilities and cross-stack browser testing boundaries.
- [`05-implementation-checkpoint-f1-f6.md`](05-implementation-checkpoint-f1-f6.md) — implemented React stack and accepted F1-F6 behavior, integration findings, remaining boundaries and cloud handoff.

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

## Current boundaries

The current executable frontend deliberately does **not** pretend that design-first future API paths already exist.

Still deferred:

- provider-backed Start / Continue / Retry clarification interaction (Backend Step 8);
- Group / Membership / Sharing UI until the Group backend is executable;
- historical Assessment deletion until Group sharing exists, because deletion is a cross-domain operation that must end active shares before hard-deleting the Assessment Session;
- dedicated Playwright E2E automation; the implemented flows are covered by Vitest/RTL/MSW and have also been exercised manually against the real Spring Boot + PostgreSQL stack.

The next active project workstream is Cloud/Delivery rather than additional frontend feature development.
