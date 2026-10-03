# Frontend

React + TypeScript frontend for the Spring AWS Portfolio project.

The application is a first-party SPA that consumes the Spring Boot backend through relative `/api/...` requests. In local development Vite proxies `/api` and `/actuator` to `http://localhost:8080`, keeping the browser-facing topology effectively same-origin and allowing the real Session/CSRF model to be exercised without adding unnecessary CORS behavior.

## Current scope

Implemented through Frontend F6:

- registration, login, Session restoration, protected routes and logout;
- CSRF token management and one-time stale-token recovery;
- Assessment catalog/detail, Start/Resume and Start New/Restart;
- Session-bound questionnaire rendering, local draft ownership, debounced full-snapshot autosave and explicit save status;
- final questionnaire submission with in-flight autosave coordination and lost-response recovery;
- backend-authoritative Assessment workflow resolution;
- deterministic clarification read/Skip/Skip Remaining interaction;
- exact-tie user decision and deterministic finalization;
- completed Result read model with per-dimension decision provenance;
- completed Assessment History with URL pagination and canonical Session detail navigation;
- Vitest + React Testing Library + MSW coverage for implemented browser behavior.

Deferred until their backend boundaries exist:

- provider-backed LLM clarification interaction;
- Group / Membership / Sharing frontend;
- historical Assessment deletion.

## Technology

- React 19
- TypeScript
- Vite
- React Router
- TanStack Query
- React Hook Form
- Vitest
- React Testing Library
- MSW

## Local development

Prerequisites:

- Node.js 24 LTS;
- Spring Boot backend running on `http://localhost:8080`;
- PostgreSQL/backend dependencies configured as described in the root README.

Install dependencies:

```bash
npm ci
```

Run the development server:

```bash
npm run dev
```

Open:

```text
http://localhost:5173
```

Vite proxies:

```text
/api      -> http://localhost:8080
/actuator -> http://localhost:8080
```

## Verification commands

```bash
npm run test
npm run build
npm run lint
```

`npm run test` uses Vitest with jsdom, React Testing Library and MSW. `npm run build` runs TypeScript project compilation before the Vite production build, so it catches type errors that a passing Vitest run alone may not detect.

## Architecture notes

The frontend follows these rules:

- `/assessment-sessions/:sessionId` is the canonical Assessment Session route;
- Questionnaire, Clarification, Tie-break and Result are presentations of backend Session state rather than separate workflow routes;
- TanStack Query owns server state; unsaved questionnaire drafts stay local;
- the browser owns the opaque HttpOnly Session cookie;
- unsafe requests obtain CSRF state through the shared HTTP client;
- mutations consume authoritative backend responses rather than manually advancing a client-side state machine;
- deterministic `4xx` query failures are not generically retried, while network/`5xx` read failures may retry once;
- design-first OpenAPI paths are not treated as executable until corresponding backend Controllers/DTOs exist.

For design and implementation history, see [`../docs/frontend/README.md`](../docs/frontend/README.md).
