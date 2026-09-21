# ADR-0014: Use Server-Side Session Authentication for the First-Party SPA

- Status: Accepted
- Date: 2026-09-20

## Context

The MVP is a first-party React SPA backed by one Spring Boot Modular Monolith. It does not currently require third-party API clients, mobile applications, cross-service token propagation, external identity federation, or stateless authentication between microservices.

Using JWT access/refresh tokens would add token storage, refresh rotation, revocation and logout semantics that do not solve a current product requirement. Keeping sessions only in one application JVM would make rolling deployment / multiple backend instances unnecessarily fragile.

## Decision

Use Spring Security server-side HTTP Session authentication and persist Session state with Spring Session JDBC in PostgreSQL.

Production browser transport uses an opaque `__Host-SESSION` cookie with `Secure`, `HttpOnly`, `SameSite=Lax`, `Path=/` and no Domain attribute. CSRF protection remains enabled; the SPA obtains a token from `GET /api/v1/auth/csrf` and sends it through `X-CSRF-TOKEN` on unsafe requests.

Spring Security establishes authenticated identity. Assessment/Group Application code keeps business authorization. Group Admin is not modeled as a global Spring Security role.

## Consequences

### Benefits

- Session lifecycle, logout and invalidation remain server-controlled.
- No access/refresh-token rotation/revocation subsystem is required for MVP.
- HttpOnly cookie prevents normal application JavaScript from reading the session identifier.
- Spring Session JDBC supports multiple backend instances without adding Redis solely for authentication.
- Authentication infrastructure remains aligned with Spring Security's standard session model.

### Costs

- Cookie-based authentication requires explicit CSRF protection.
- Session persistence adds technical tables and database reads/writes.
- Truly stateless third-party API/mobile authentication would require a future authentication design change.

## Alternatives considered

### JWT access + refresh tokens

Deferred because the current first-party SPA does not benefit enough to justify refresh rotation, revocation and browser credential-storage complexity.

### In-memory HttpSession only

Rejected because authentication would be tied to one JVM and would complicate multi-instance/rolling deployment.

### Spring Session Redis

Viable later if session load justifies it, but PostgreSQL already exists in the MVP and is sufficient for current scale.
