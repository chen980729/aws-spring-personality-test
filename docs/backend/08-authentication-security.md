# Authentication & Security Design

> **Status:** Accepted MVP Detailed Design  
> **Last updated:** 2026-09-20

## 1. Decision summary

MVP authentication uses:

```text
Email + Password
+ Spring Security
+ server-side HTTP Session
+ Spring Session JDBC
+ PostgreSQL
+ Secure / HttpOnly session cookie
+ CSRF protection
```

The MVP deliberately does **not** use JWT access/refresh tokens, `localStorage` credentials, OAuth/OIDC, MFA, Remember Me, device/session management, or social login.

This is a first-party React SPA talking to one Spring Boot Modular Monolith. Server-side session authentication keeps credential/session lifecycle server-controlled and avoids adding refresh-token rotation/revocation complexity that the MVP does not need.

## 2. Session persistence

Use Spring Session JDBC so authentication state is not tied to one JVM and can survive rolling deployment / multiple backend instances.

Session state is technical infrastructure, not an Identity Domain Aggregate.

Flyway owns the Spring Session schema as it owns the rest of the database schema. `V2__create_spring_session_tables.sql` is based on the official PostgreSQL schema for the pinned Spring Session JDBC version. Production startup must not auto-create session tables; configure schema initialization as `never`.

## 3. Password handling

Login identifier is canonicalized email. MVP canonicalization is one shared function used by both registration and login:

```text
trim surrounding whitespace
-> lowercase with Locale.ROOT
-> validate canonical email syntax
-> store / query canonical value
```

Password is never trimmed/lowercased/normalized by the server.

MVP password policy:

```text
minimum length: 15
maximum length: 128
no mandatory character-composition rule
```

Passwords are stored only as an adaptive one-way hash. MVP chooses Argon2id behind an Identity Application output port such as `PasswordHasher`; Domain code does not depend on Spring Security encoders.

## 4. Principal and authorization boundary

`SecurityContext` stores a minimal authenticated principal containing only stable identity such as:

```text
userId
email
```

Spring Security answers:

```text
Who is authenticated?
```

Assessment / Group Application logic answers:

```text
May this actor perform this business action?
```

`Group Admin` is a per-Group Domain fact and is **not** represented as a global Spring Security role.

Spring Security types stop at the Web boundary. Controllers translate the principal into `UserId` and pass it explicitly to Application commands/queries.

## 5. Registration and login

Business surface:

```text
GET  /api/v1/auth/csrf
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/users/me
```

Registration requires a non-empty human-readable `displayName` in addition to email/password. `displayName` is user-facing identity data that may be exposed in Group member/join/share read models; email remains private account/authentication data.

Registration does not automatically log the user in.

JSON login uses Spring Security's `AuthenticationManager` and explicitly persists the successful `SecurityContext` through the configured `SecurityContextRepository` so authentication survives subsequent requests.

Successful authentication uses Spring Security session-fixation protection; the session identifier is rotated rather than reusing the pre-authentication identifier.

Unknown-email and wrong-password failures expose the same public error:

```text
401 INVALID_CREDENTIALS
```

## 6. Session cookie

Production cookie policy:

```text
name: __Host-SESSION
Secure: true
HttpOnly: true
SameSite: Lax
Path: /
Domain: not set
Max-Age: not set (session cookie)
```

Development may use a non-Secure `SESSION` cookie on localhost; this profile difference is transport-only and does not change authorization behavior.

MVP idle timeout is 30 minutes. Remember-me is disabled.

## 7. CSRF

Because the browser automatically attaches the session cookie, CSRF protection remains enabled for unsafe methods.

Use server-side CSRF token storage with an explicit bootstrap endpoint:

```text
GET /api/v1/auth/csrf
```

The SPA sends the returned token in:

```text
X-CSRF-TOKEN
```

for `POST`, `PUT`, `PATCH`, and `DELETE` requests, including anonymous registration/login requests.

The SPA re-fetches the CSRF token on initial load and after login/logout lifecycle changes. It may keep the token only in JavaScript memory; it does not need `localStorage` or `sessionStorage`.

Missing/invalid CSRF returns:

```text
403 CSRF_VALIDATION_FAILED
```

## 8. Logout and session expiry

`POST /api/v1/auth/logout` invalidates the current server-side Session, clears authentication and expires the session cookie. It requires CSRF protection.

For a safe protected request, an expired or missing authenticated Session returns:

```text
401 AUTHENTICATION_REQUIRED
```

For an unsafe request (`POST`/`PUT`/`PATCH`/`DELETE`), Spring Security may reject missing/expired CSRF state first and return:

```text
403 CSRF_VALIDATION_FAILED
```

The SPA may confirm authentication with `GET /api/v1/users/me` and bootstrap a fresh token through `GET /api/v1/auth/csrf` as appropriate. The API contract therefore permits both 401 and 403 on protected unsafe operations.

There is no refresh-token flow. The SPA returns to anonymous/login state when authentication is no longer valid.

## 9. HTTP security errors

API security never returns Spring's HTML login form or redirects.

Use Problem Details responses through custom Spring Security integration:

```text
AuthenticationEntryPoint -> 401 application/problem+json
AccessDeniedHandler      -> 403 application/problem+json
```

Business authorization remains in Application code and follows the REST contract's 403-vs-404 privacy rule.

## 10. Same-origin deployment preference

Production prefers one browser origin:

```text
https://portfolio.example.com/
    /       -> React frontend
    /api/*  -> Spring Boot backend
```

React uses relative `/api/v1/...` URLs. Local Vite development should use a dev proxy where practical.

Do not enable wildcard credentialed CORS. If a future deployment truly requires cross-origin frontend/backend access, configure exact allowed origins deliberately.

## 11. Security hardening boundary

Never log plaintext passwords, password hashes, session IDs/cookies, or CSRF tokens.

Production should add ingress-level throttling/rate limiting around login/register. MVP does not add permanent account lockout counters to `UserAccount`.

HTTPS is mandatory in production; public-edge HSTS/TLS details belong to AWS Infrastructure Design.

## 12. Deferred security features

Deferred until a real requirement exists:

```text
email verification
password reset/change
MFA
OAuth/OIDC/social login
remember-me
device/session dashboard
logout-all-devices
API keys
machine-to-machine authentication
account recovery
```

Account deletion remains deferred and requires a dedicated policy across Assessment ownership, Group authority/history, Membership, JoinRequest, Share history, and anonymization/deletion semantics.

## 13. Testing expectations

At minimum verify:

- registration stores canonical email + displayName + password hash and rejects duplicate canonical email;
- valid login creates authenticated Session;
- unknown-email and wrong-password produce the same `401 INVALID_CREDENTIALS`;
- session ID changes on login;
- Spring Session JDBC allows authentication to be read by another app instance using the same DB;
- logout invalidates the Session;
- safe protected requests with expired Session produce Problem Details 401; unsafe requests may first produce CSRF 403 as documented;
- unsafe request without/with invalid CSRF is 403;
- valid CSRF succeeds and a fresh token works after login/logout;
- anonymous Assessment/Group APIs are 401;
- Group business authorization still occurs in Application code;
- production cookie configuration is Secure + HttpOnly + SameSite=Lax + Path=/.
