# Frontend Authentication, Session and CSRF Boundary

> **Original checkpoint:** Frontend F0
> **Status:** Accepted and implemented through Frontend F2; reused by later Assessment flows
> **Original date:** 2026-09-30
> **Implementation review:** 2026-10-03
> **Related ADR:** [`ADR-0014`](../adr/ADR-0014-server-side-session-authentication.md)

## 1. Authentication model

The first-party SPA uses Spring Security server-side Session authentication persisted through Spring Session JDBC.

The browser owns the opaque Session cookie. React must not read, copy or persist the Session identifier.

Do not introduce JWT/localStorage token behavior for this frontend.

## 2. Current local vs production cookie configuration

Current local backend configuration intentionally uses:

```text
SESSION
HttpOnly=true
Secure=false
SameSite=Lax
Path=/
```

This is appropriate for local HTTP development.

ADR-0014 freezes production transport as:

```text
__Host-SESSION
HttpOnly
Secure
SameSite=Lax
Path=/
no Domain
```

The production cookie switch belongs to environment/deployment configuration, not frontend JavaScript.

## 3. Same-origin frontend topology

Development should use a Vite `/api` proxy:

```text
Browser http://localhost:5173
        |
        | /api/v1/*
        v
Vite dev proxy
        |
        v
Spring Boot http://localhost:8080
```

Frontend code always calls relative URLs such as:

```text
/api/v1/users/me
```

Do not hard-code `http://localhost:8080` into feature code.

This keeps the browser-facing topology close to the intended production same-origin application and avoids adding cross-origin/CORS complexity that the MVP does not require.

The shared HTTP client should explicitly use `credentials: "same-origin"` to make the cookie-session dependency clear.

## 4. Authentication server state

The current user is server state represented by:

```text
GET /api/v1/users/me
```

Frontend authentication has three useful UI states:

```text
UNKNOWN       application bootstrap/query pending
AUTHENTICATED current-user data exists
ANONYMOUS     authentication-required response
```

Do not persist `loggedIn=true` or equivalent browser flags.

Any API response with `AUTHENTICATION_REQUIRED` must cause the frontend authentication cache to become anonymous so the authenticated route boundary can react consistently.

Route guards improve UX; Spring Security remains the actual security boundary.

## 5. CSRF token ownership

The CSRF token is protocol state, not component state or normal TanStack Query server state.

Use a small in-memory `CsrfTokenManager` under `shared/api`.

Conceptually it owns:

```text
getToken()
refresh()
invalidate()
```

It should support one shared in-flight refresh so concurrent unsafe requests do not each request a new token independently.

Never persist the CSRF token in `localStorage` or `sessionStorage`.

## 6. Unsafe request flow

Unsafe methods include POST, PUT, PATCH and DELETE.

```text
unsafe API request
      ↓
obtain cached CSRF token
or GET /api/v1/auth/csrf
      ↓
X-CSRF-TOKEN: <token>
      ↓
request
```

GET/HEAD requests do not require the CSRF request header.

## 7. Login boundary and CSRF rotation

The backend login flow uses both:

```text
ChangeSessionIdAuthenticationStrategy
CsrfAuthenticationStrategy
```

Therefore login success is an authentication/CSRF rotation boundary.

Accepted frontend sequence:

```text
GET /auth/csrf
      ↓
POST /auth/login
      ↓
login success + authenticated user response
      ↓
update current-user query cache
      ↓
invalidate cached CSRF token
      ↓
GET /auth/csrf again
      ↓
cache authenticated-session CSRF token
```

Registration creates a user but does not authenticate the browser. The first frontend UX may redirect a successful registration to Login.

## 8. Logout boundary

On successful logout:

```text
POST /auth/logout
      ↓
Session invalidated
      ↓
clear/mark current-user cache anonymous
      ↓
invalidate CSRF token
```

A new anonymous CSRF token can be acquired lazily before the next unsafe anonymous operation.

If the Session was already gone and logout observes `AUTHENTICATION_REQUIRED`, the feature-level outcome is still anonymous; the HTTP infrastructure must not rewrite the response itself.

## 9. CSRF failure recovery

An unsafe request with a stale token may be rejected in Spring Security before authentication/business code runs.

Therefore an expired Session can first appear as:

```text
403 CSRF_VALIDATION_FAILED
```

rather than immediately as 401.

Accepted shared HTTP recovery:

```text
CSRF_VALIDATION_FAILED
      ↓
invalidate cached token
      ↓
GET /auth/csrf
      ↓
retry original request exactly once
```

Retrying once is safe at this protocol boundary because the failed CSRF request did not reach the Controller/business mutation.

Never loop indefinitely.

A representative expired-session path is:

```text
old Session + old CSRF
      ↓
unsafe request
      ↓
403 CSRF_VALIDATION_FAILED
      ↓
refresh anonymous CSRF
      ↓
retry once
      ↓
401 AUTHENTICATION_REQUIRED
      ↓
auth cache becomes anonymous
      ↓
login route with preserved returnTo
```

`403 ACCESS_DENIED` is different and must not be treated as CSRF recovery.

## 10. `returnTo`

When authentication is required for a protected route, Login may preserve the original internal route through a query parameter, for example:

```text
/login?returnTo=/assessment-sessions/<id>
```

The value must be sanitized as an internal application path. External URLs are rejected/fallback to a safe default such as `/assessments` to avoid an open-redirect vulnerability.


## 11. Implementation verification

The F2 implementation now exercises this boundary through real feature code:

- `CsrfTokenManager` keeps protocol state in memory and shares an in-flight refresh;
- the shared HTTP client attaches CSRF to unsafe requests and retries `CSRF_VALIDATION_FAILED` exactly once;
- authentication-required events synchronize unexpected Session expiry with the TanStack Query current-user cache;
- `AuthenticatedLayout` protects Assessment/History routes;
- Login/Register preserve only sanitized internal `returnTo` paths;
- Logout treats an already-expired Session as a converged anonymous outcome and invalidates cached CSRF state.

Browser integration also exposed a backend principal-name edge case: Spring Session persists `Authentication#getName()` in its indexed principal column. `AuthenticatedUserPrincipal` now implements Spring Security `AuthenticatedPrincipal` and returns the immutable `userId` string as its name rather than relying on a record `toString()`. This keeps the standard Spring Session schema valid and makes principal identity stable regardless of email length.
