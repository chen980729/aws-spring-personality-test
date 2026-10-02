# Frontend Documentation

> **Current checkpoint:** Frontend F0 complete — Integration Baseline + Architecture
> **Date:** 2026-09-30
> **Next step:** F1 — React + TypeScript Foundation

This directory records the accepted frontend design baseline before implementation begins.

The frontend is not treated as a second source of business truth. It expresses user intent, renders backend-authoritative state, and provides browser-level integration across Session/CSRF, routing, recovery, autosave, and assessment workflow behavior.

## F0 accepted artifacts

- [`01-frontend-architecture.md`](01-frontend-architecture.md) — route/page boundaries, feature ownership, project structure, and Assessment workflow rendering model.
- [`02-api-integration.md`](02-api-integration.md) — executable API baseline, target-vs-runtime contract distinction, server-state ownership, mutation/recovery rules, and Problem Details usage.
- [`03-auth-session-csrf.md`](03-auth-session-csrf.md) — browser Session/CSRF protocol, same-origin development topology, login/logout rotation behavior, and authentication recovery.
- [`04-testing-strategy.md`](04-testing-strategy.md) — Vitest/RTL/MSW/Playwright responsibilities and cross-stack E2E boundaries.

## F0 architecture summary

```text
Browser / React
      |
      | stable routes + user intent
      v
Frontend feature layer
      |
      | TanStack Query server state
      | local draft/UI state
      v
shared/api HTTP boundary
      |
      | relative /api/v1/*
      | Session cookie managed by browser
      | X-CSRF-TOKEN on unsafe requests
      v
Spring Boot
      |
      v
PostgreSQL
```

The first implementation vertical slice is:

```text
Register / Login / Session
        ↓
Assessment Catalog / Detail
        ↓
Start or Resume / Start New
        ↓
Questionnaire / Autosave / Refresh Recovery
        ↓
Submit
        ↓
Clarification Skip / Tie-break
        ↓
Result / History
```

Real provider-backed AI clarification remains Backend Step 8 and will extend the existing `ClarificationView` rather than create a separate frontend architecture.
