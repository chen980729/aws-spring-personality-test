# Identity Authentication Implementation Checkpoint

> **Status:** Implemented / Runtime-verified checkpoint  
> **Checkpoint date:** 2026-09-23  
> **Scope:** Identity registration, authentication, server-side Session, CSRF, current-user query and logout  
> **Next backend slice:** Assessment vertical slice

This document records the implementation state reached after completing the first end-to-end Identity/Security slice. It complements the frozen design in `08-authentication-security.md`; it does not replace that design baseline.

## 1. Checkpoint summary

The backend now supports the complete MVP authentication lifecycle:

```text
Register
  -> Login
  -> authenticated server-side Session
  -> CSRF-protected state-changing requests
  -> GET /api/v1/users/me
  -> Logout
  -> Session invalidated
  -> protected request returns 401
```

Implemented HTTP surface:

```text
GET  /api/v1/auth/csrf
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/users/me
```

The implementation follows the accepted decisions from `08-authentication-security.md`:

- email + password authentication;
- Argon2id password hashing;
- Spring Security;
- server-side HTTP Session;
- Spring Session JDBC backed by PostgreSQL;
- CSRF protection for unsafe methods;
- minimal authenticated principal;
- RFC 9457 Problem Details for Web/Security errors;
- registration does not automatically authenticate the new user.

## 2. Implemented architecture

```text
┌──────────────────────────────────────────────────────────────┐
│                           Client                             │
│                    React SPA / curl                          │
└──────────────────────────────┬───────────────────────────────┘
                               │ HTTP + SESSION cookie
                               │ X-CSRF-TOKEN on unsafe methods
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                    Spring Security Filter Chain              │
│                                                              │
│  CSRF validation                                             │
│  Authentication / authorization                              │
│  LogoutFilter                                                │
│  ApiAuthenticationEntryPoint                                 │
│  ApiAccessDeniedHandler                                      │
└──────────────────────┬───────────────────────┬───────────────┘
                       │                       │
           MVC request │                       │ Session / SecurityContext
                       ▼                       ▼
┌─────────────────────────────────┐  ┌─────────────────────────┐
│             Web                 │  │   Security Infrastructure│
│                                 │  │                         │
│ IdentityAuthController          │  │ AuthenticationManager   │
│ IdentityUserController          │  │ ApplicationAuthProvider │
│ IdentityExceptionHandler        │  │ SecurityContextRepo     │
│ GlobalWebExceptionHandler       │  │ SessionAuthStrategy     │
└───────────────┬─────────────────┘  │ CSRF Token Repository   │
                │                    │ Logout Success Handler  │
                │                    └────────────┬────────────┘
                │                                 │
                └────────────────┬────────────────┘
                                 ▼
┌──────────────────────────────────────────────────────────────┐
│                     Application Layer                       │
│                                                              │
│ RegisterUserService                                          │
│ AuthenticateUserService                                      │
│ GetCurrentUserService                                        │
│ EmailCanonicalizer                                           │
│ PasswordHasher (output port)                                 │
│ UserAccountRepository (domain repository port)               │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                         Domain                              │
│                                                              │
│ UserId                                                       │
│ UserAccount                                                  │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                       Infrastructure                         │
│                                                              │
│ Argon2PasswordHasher                                         │
│ JpaUserAccountRepositoryAdapter                              │
│ UserAccountPersistenceMapper                                 │
│ SpringDataUserAccountRepository                              │
│ UserAccountJpaEntity                                         │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                         PostgreSQL                           │
│                                                              │
│ user_accounts                                                │
│ spring_session                                               │
│ spring_session_attributes                                    │
│ flyway_schema_history                                        │
└──────────────────────────────────────────────────────────────┘
```

The important dependency direction remains:

```text
Web / Security Infrastructure
          ↓
      Application
          ↓
        Domain
          ↑
Infrastructure adapters implement Domain/Application ports
```

Spring Security types are kept out of the Domain and Application use cases. The Security integration is an adapter around the Application authentication use case.

## 3. Registration flow

Endpoint:

```text
POST /api/v1/auth/register
```

Runtime path:

```text
Client
  │
  │ JSON + X-CSRF-TOKEN
  ▼
SecurityFilterChain
  │ CSRF validation
  ▼
IdentityAuthController
  ▼
RegisterUserService
  ├─ canonicalize email
  ├─ validate registration password policy
  ├─ check duplicate canonical email
  ├─ PasswordHasher.hash(rawPassword)
  ├─ UserAccount.create(...)
  └─ UserAccountRepository.save(...)
  ▼
JpaUserAccountRepositoryAdapter
  ▼
JPA / Hibernate
  ▼
PostgreSQL.user_accounts
  ▼
201 Created
```

Key behavior:

- registration uses the same canonical email representation that login later queries;
- the raw password is never persisted;
- `password_hash` stores an Argon2id hash;
- registration does not establish an authenticated Session;
- duplicate canonical email is rejected by the Application flow, while the database unique constraint remains the concurrency backstop.

## 4. CSRF bootstrap flow

Endpoint:

```text
GET /api/v1/auth/csrf
```

Runtime path:

```text
Client
  ▼
SecurityFilterChain
  ▼
HttpSessionCsrfTokenRepository
  │
  ├─ create/load Session when required
  └─ associate CSRF token with Session
  ▼
IdentityAuthController
  ▼
CsrfTokenResponse
  ▼
200 OK
```

The client sends the returned token as:

```text
X-CSRF-TOKEN
```

for `POST`, `PUT`, `PATCH`, and `DELETE` requests.

The Session is persisted through Spring Session JDBC instead of being tied to in-memory state in one JVM.

## 5. Login and Session establishment flow

Endpoint:

```text
POST /api/v1/auth/login
```

Runtime path:

```text
Client
  │ JSON + existing anonymous SESSION + X-CSRF-TOKEN
  ▼
SecurityFilterChain
  │ CSRF validation
  ▼
IdentityAuthController
  ▼
UsernamePasswordAuthenticationToken (unauthenticated)
  ▼
AuthenticationManager
  ▼
ApplicationAuthenticationProvider
  ▼
AuthenticateUserService
  ├─ canonicalize email
  ├─ UserAccountRepository.findByEmail(...)
  └─ PasswordHasher.matches(rawPassword, storedHash)
  ▼
AuthenticatedUserPrincipal
  ▼
UsernamePasswordAuthenticationToken (authenticated)
  ▼
SessionAuthenticationStrategy
  ├─ ChangeSessionIdAuthenticationStrategy
  └─ CsrfAuthenticationStrategy
  ▼
SecurityContext
  ▼
SecurityContextRepository.saveContext(...)
  ▼
Spring Session JDBC
  ▼
PostgreSQL.spring_session / spring_session_attributes
  ▼
200 OK
```

The login boundary deliberately separates three responsibilities:

```text
AuthenticateUserService
    -> business/application authentication decision

ApplicationAuthenticationProvider
    -> Spring Security adapter

IdentityAuthController + Session infrastructure
    -> establish/persist authenticated HTTP Session
```

### 5.1 Credential failure semantics

Unknown email and wrong password both become:

```text
401 INVALID_CREDENTIALS
```

This avoids exposing whether an email is registered.

### 5.2 Session fixation protection

On successful login the pre-authentication Session identifier is rotated using `ChangeSessionIdAuthenticationStrategy`.

Conceptually:

```text
before login: SESSION=A
        ↓ authentication succeeds
 after login: SESSION=B
```

The application does not continue using the anonymous Session identifier as the authenticated identifier.

### 5.3 CSRF lifecycle after login

`CsrfAuthenticationStrategy` invalidates the pre-login CSRF token. The client obtains a fresh token after successful login:

```text
POST /auth/login
  -> authentication succeeds
  -> old CSRF token cleared
  -> GET /auth/csrf
  -> new authenticated-session CSRF token
```

## 6. Current-user flow

Endpoint:

```text
GET /api/v1/users/me
```

Runtime path:

```text
Client + SESSION cookie
  ▼
Spring Session JDBC
  ▼
SecurityContext restored
  ▼
SecurityFilterChain requires authenticated actor
  ▼
IdentityUserController
  ▼
@AuthenticationPrincipal AuthenticatedUserPrincipal
  ▼
UserId
  ▼
GetCurrentUserService
  ▼
UserAccountRepository.findById(...)
  ▼
CurrentUserResponse
  ▼
200 OK
```

The authenticated principal is intentionally minimal:

```text
userId
email
```

Mutable profile/read-model data such as `displayName` is read through the Application layer rather than copied into the Session. This keeps the Session focused on authentication identity and avoids stale profile data.

An anonymous request does not enter the Controller. The Security layer returns:

```text
401 AUTHENTICATION_REQUIRED
```

## 7. Logout flow

Endpoint:

```text
POST /api/v1/auth/logout
```

Logout is handled by Spring Security's logout infrastructure rather than by a custom MVC Controller.

Runtime path:

```text
Client
  │ SESSION cookie + X-CSRF-TOKEN
  ▼
SecurityFilterChain
  │ CSRF validation
  ▼
LogoutFilter
  ├─ clear Authentication
  ├─ clear SecurityContext
  ├─ invalidate HttpSession
  └─ invoke ApiLogoutSuccessHandler
  ▼
Spring Session JDBC
  ▼
remove invalidated Session from PostgreSQL
  ▼
204 No Content
```

After logout, the old Session can no longer authenticate a protected request:

```text
GET /api/v1/users/me
  -> 401 AUTHENTICATION_REQUIRED
```

The SPA should bootstrap a fresh anonymous CSRF token after logout.

## 8. Security error flow

Security failures occur before MVC Controllers in many cases. They therefore use explicit adapters into the common Problem Details mechanism:

```text
Unauthenticated protected request
  ▼
ApiAuthenticationEntryPoint
  ▼
HandlerExceptionResolver
  ▼
GlobalWebExceptionHandler
  ▼
401 application/problem+json
code = AUTHENTICATION_REQUIRED
```

```text
CSRF / access denied
  ▼
ApiAccessDeniedHandler
  ▼
HandlerExceptionResolver
  ▼
GlobalWebExceptionHandler
  ▼
403 application/problem+json
```

CSRF failures expose:

```text
code = CSRF_VALIDATION_FAILED
```

This preserves one API error format even though some failures originate in Spring Security Filters and others originate in MVC/Application code.

## 9. Persistence ownership

Flyway remains the sole schema owner.

Current Identity/Security tables:

```text
user_accounts
spring_session
spring_session_attributes
flyway_schema_history
```

Ownership is intentionally different:

```text
user_accounts
  -> Identity application/domain persistence

spring_session / spring_session_attributes
  -> Spring Session technical infrastructure
```

There are no JPA Domain entities for Spring Session tables. Spring Session JDBC owns their runtime access.

## 10. Confirmed implementation decisions

The implementation has confirmed the following design decisions without requiring an architecture reversal:

1. **Session authentication remains appropriate for the MVP.** It avoids token refresh/revocation infrastructure while still supporting the first-party SPA.
2. **Spring Session JDBC gives the Session an explicit persistence boundary.** Authentication state is not tied only to one process memory space.
3. **CSRF remains required.** The browser automatically sends the Session cookie, therefore unsafe requests need anti-CSRF state.
4. **Application authentication remains independent of Spring Security.** Spring Security is integrated through `ApplicationAuthenticationProvider` rather than pushed into `AuthenticateUserService`.
5. **Minimal Session principal remains appropriate.** Business/profile reads continue through Application services.
6. **Logout belongs in Security infrastructure.** Spring Security's logout pipeline is used rather than reimplementing Session/SecurityContext cleanup in a Controller.
7. **Problem Details is shared across MVC and Security failures.** Custom Security handlers delegate into the same exception-resolution boundary.

## 11. Runtime verification completed at this checkpoint

The following behavior has been exercised successfully during local implementation:

```text
obtain CSRF token
  -> register user
  -> persisted Argon2id password hash
  -> login with registered credentials
  -> authenticated Session established
  -> protected current-user request succeeds
  -> login/session CSRF lifecycle behaves as designed
  -> logout succeeds
  -> logged-out Session no longer authenticates protected request
```

PostgreSQL/Flyway and Testcontainers-backed persistence tests also run successfully at this checkpoint.

This checkpoint does **not** claim that every deferred security hardening concern is complete. Rate limiting, MFA, password reset/change, OAuth/OIDC, multi-device management and account recovery remain outside the current MVP slice.

## 12. Next implementation step

Identity/Security is now sufficient to support business modules. The next backend implementation slice should follow `07-implementation-handoff.md` Phase 4:

```text
Assessment
  -> list assessment
  -> start / resume session
  -> restart / start new
  -> save questionnaire
  -> submit
  -> deterministic result
  -> history
```

Do not continue expanding Identity unless the Assessment/Group implementation exposes a concrete missing capability.
