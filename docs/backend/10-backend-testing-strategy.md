# Backend Testing Strategy & Current Coverage

> **Status:** Active implementation guidance  
> **Last reviewed:** 2026-09-23  
> **Current coverage checkpoint:** Identity/Security authentication lifecycle

## 1. Testing principle

The backend uses layered tests so that each test answers one clear question.

```text
Domain Unit Test
      ↓
Application Unit Test
      ↓
Infrastructure / Adapter Unit Test
      ↓
Web MVC Slice Test
      ↓
Persistence Integration Test
      ↓
Focused end-to-end/runtime verification
```

The goal is **not** to make every test boot the whole application. Most business behavior should fail fast in small deterministic tests; integration tests are used where framework/database behavior is the thing being verified.

This supports the backend architecture:

```text
Web -> Application -> Domain
               ↑
     Infrastructure adapters
```

Each layer can therefore be tested at its own boundary.

## 2. Domain unit tests

### Current representative test

```text
UserAccountTest
```

Responsibilities:

- validate `UserAccount` construction invariants;
- reject invalid state such as blank required fields;
- exercise pure Domain behavior without Spring or PostgreSQL.

Characteristics:

```text
Spring Context: no
Database:       no
Mockito:        usually no
Speed:          very fast
```

Domain tests should prefer real Domain values over mocks.

## 3. Application unit tests

### Current representative tests

```text
RegisterUserServiceTest
AuthenticateUserServiceTest
```

These tests verify use-case orchestration with mocked output ports.

### Register coverage

Current checks include:

- successful registration;
- canonical email is used;
- duplicate canonical email is rejected;
- password policy failure is rejected;
- successful registration passes a hash, not the raw password, into the Domain/persistence boundary;
- failed registration does not perform unnecessary downstream work.

### Authenticate coverage

Current checks include:

- canonicalized email lookup;
- correct password succeeds;
- unknown email becomes `InvalidCredentialsException`;
- wrong password becomes the same `InvalidCredentialsException`;
- unknown user does not attempt password verification against a non-existent stored hash.

Characteristics:

```text
Spring Context: no
Database:       no
Mockito:        yes, for repository/output ports
Purpose:        application workflow and branch behavior
```

Application tests should not re-test Argon2 implementation details or JPA mappings.

## 4. Security adapter unit tests

### `Argon2PasswordHasherTest`

Verifies the concrete password-hashing adapter:

- encoded value differs from plaintext;
- output uses Argon2id format;
- correct raw password matches;
- incorrect raw password does not match.

This test intentionally exercises the real crypto adapter rather than mocking it.

### `ApplicationAuthenticationProviderTest`

Verifies the Spring Security -> Application bridge:

```text
UsernamePasswordAuthenticationToken
        ↓
ApplicationAuthenticationProvider
        ↓
AuthenticateUserService
        ↓
AuthenticatedUserPrincipal
        ↓
authenticated Authentication
```

Current checks include:

- valid Application result becomes authenticated Spring Security `Authentication`;
- principal contains the expected stable identity;
- credentials are not retained after successful authentication;
- `InvalidCredentialsException` is translated to `BadCredentialsException`;
- the provider supports the expected authentication token type.

These tests do not test PostgreSQL or actual Argon2 matching; those responsibilities are covered elsewhere.

## 5. Web MVC slice tests

Web tests use `MockMvc` and a focused `@WebMvcTest` slice.

Controller tests are kept aligned with Controller ownership:

```text
IdentityAuthControllerTest
  -> /api/v1/auth/* behavior

IdentityUserControllerTest
  -> /api/v1/users/* behavior
```

A useful rule learned during implementation is:

> A `@WebMvcTest(SomeController.class)` loads the selected MVC slice, not every Controller in the application.

Therefore a request to an endpoint owned by another Controller can correctly produce `404` in the test context even though the production application contains that endpoint. Controller-specific test classes keep this boundary explicit.

### 5.1 Registration Web coverage

Current checks include:

```text
valid request + CSRF
  -> 201

invalid request validation
  -> 400 Problem Details

missing CSRF
  -> 403 CSRF_VALIDATION_FAILED
```

### 5.2 Login Web coverage

Current checks include:

```text
valid login
  -> 200
  -> SessionAuthenticationStrategy invoked
  -> authenticated Authentication stored in SecurityContext
  -> SecurityContextRepository.saveContext invoked

invalid credentials
  -> 401 INVALID_CREDENTIALS
  -> no authenticated Session setup

missing CSRF
  -> 403 CSRF_VALIDATION_FAILED
  -> AuthenticationManager not invoked
```

The login Controller test deliberately mocks `AuthenticationManager`. Password validation itself belongs to the Application/Security adapter tests.

### 5.3 Current-user Web coverage

Current checks include:

```text
authenticated principal
  -> GET /api/v1/users/me
  -> 200 + current user response

anonymous request
  -> 401 AUTHENTICATION_REQUIRED
  -> Application current-user query is not invoked
```

The authenticated test uses the project-specific `AuthenticatedUserPrincipal`, not Spring Security's generic test `UserDetails`, so it matches the production Controller contract.

### 5.4 Logout Web coverage

Current checks include:

```text
authenticated + valid CSRF
  -> 204

authenticated + missing CSRF
  -> 403 CSRF_VALIDATION_FAILED

anonymous + valid CSRF
  -> 401 AUTHENTICATION_REQUIRED
```

Logout is handled by Spring Security infrastructure, not an MVC Controller, so these tests verify SecurityFilterChain behavior around the configured logout endpoint.

## 6. Persistence integration tests

### Current representative test

```text
JpaUserAccountRepositoryAdapterIntegrationTest
```

The integration test uses a real PostgreSQL Testcontainers instance and verifies the actual persistence boundary.

Coverage includes:

- Spring Boot wiring;
- Testcontainers connection integration;
- Flyway migrations execute against PostgreSQL;
- Hibernate/JPA mapping validates against the Flyway-owned schema;
- save/load `UserAccount` through the repository adapter;
- find by ID;
- find by canonical email;
- email existence query.

Characteristics:

```text
Spring Context: yes
Database:       real PostgreSQL container
Flyway:         yes
JPA/Hibernate:  yes
Speed:          slower than unit tests
```

This is intentionally different from an H2/in-memory test. PostgreSQL-specific schema/mapping behavior should be verified against PostgreSQL.

## 7. Runtime verification

In addition to automated tests, the Identity checkpoint has been exercised through the real running application.

Verified sequence:

```text
GET /api/v1/auth/csrf
  -> obtain CSRF token + Session

POST /api/v1/auth/register
  -> create user
  -> inspect persisted account/hash

POST /api/v1/auth/login
  -> authenticate credentials
  -> establish authenticated Session
  -> rotate Session identifier
  -> invalidate pre-login CSRF token

GET /api/v1/auth/csrf
  -> obtain fresh authenticated-session CSRF token

GET /api/v1/users/me
  -> restore authentication from Session
  -> return current user

POST /api/v1/auth/logout
  -> invalidate Session

GET /api/v1/users/me
  -> 401 after logout
```

Manual/runtime verification is useful for understanding the end-to-end Session lifecycle, but it is not a replacement for repeatable automated tests.

## 8. Identity/Security coverage matrix

| Behavior | Domain Unit | Application Unit | Security Adapter Unit | Web MVC | Persistence Integration | Runtime verified |
| --- | --- | --- | --- | --- | --- | --- |
| UserAccount invariants | Yes | — | — | — | — | — |
| Email canonicalization on register | — | Yes | — | Indirect | — | Yes |
| Password registration policy | — | Yes | — | Yes (transport validation cases) | — | Yes |
| Argon2id hashing/matching | — | — | Yes | — | — | Yes |
| Duplicate email application handling | — | Yes | — | Not the primary test | DB constraint covered by schema | Yes |
| Repository save/find | — | — | — | — | Yes | Yes |
| Unknown email / wrong password same app error | — | Yes | — | — | — | Yes |
| Application auth -> Spring Authentication | — | — | Yes | — | — | Yes |
| Login HTTP success | — | — | — | Yes | — | Yes |
| Invalid credentials -> 401 | — | — | Yes translation | Yes | — | Yes |
| Missing CSRF -> 403 | — | — | — | Yes | — | Yes |
| SecurityContext saved on login | — | — | — | Yes | — | Yes |
| Session ID rotation | — | — | — | Strategy invocation is covered | — | Yes |
| `/users/me` authenticated | — | Application query dependency | — | Yes | Repository separately covered | Yes |
| `/users/me` anonymous -> 401 | — | — | — | Yes | — | Yes |
| Logout authenticated -> 204 | — | — | — | Yes | — | Yes |
| Logout without CSRF -> 403 | — | — | — | Yes | — | Yes |
| Old Session unusable after logout | — | — | — | Partial | — | Yes |
| Flyway + JPA + PostgreSQL compatibility | — | — | — | — | Yes | Yes |

`—` means that layer is not the intended owner of that behavior, not that the feature is untested overall.

## 9. Known test gaps at this checkpoint

The current coverage is sufficient to continue to the Assessment implementation, but several tests remain useful future hardening work.

### 9.1 Automated full authentication integration test

There is not yet a single automated test that boots the full application stack and executes:

```text
csrf -> register -> login -> /me -> logout -> /me=401
```

The flow has been runtime-verified manually. A small number of full-stack integration tests may be added later, particularly before CI/CD/AWS deployment, but they should not replace the existing focused tests.

### 9.2 Cross-instance Spring Session verification

The accepted security design expects Spring Session JDBC to permit multiple backend instances to share authenticated Session state through the same PostgreSQL database.

The current implementation structure supports this, but a dedicated automated two-instance test has not been established at this checkpoint. This becomes more valuable when the deployment topology introduces multiple application instances.

### 9.3 Production cookie configuration verification

Local development intentionally uses a non-Secure localhost Session cookie. Production requirements remain:

```text
__Host-SESSION
Secure=true
HttpOnly=true
SameSite=Lax
Path=/
```

A profile/configuration test should be added when production/AWS configuration is introduced.

### 9.4 Concurrency/security hardening

Not yet covered at this Identity checkpoint:

- concurrent duplicate registration race at the HTTP/use-case level;
- rate limiting / brute-force protection;
- multi-device/session-management behavior;
- account recovery/password reset because those features are deferred.

## 10. Test authoring rules going forward

For Assessment and Group implementation, follow these rules:

1. **Start with Domain/Application tests for business rules.** Do not require Spring to test pure lifecycle/invariant behavior.
2. **Mock ports, not internal implementation details.** Application tests mock repositories/external output ports.
3. **Use PostgreSQL Testcontainers for persistence constraints and concurrency.** Do not substitute H2 for PostgreSQL-specific behavior.
4. **Use `@WebMvcTest` for HTTP mapping, validation and Security/Web behavior.** Keep each Controller test aligned with the selected MVC slice.
5. **Do not duplicate responsibility across layers.** For example, Controller tests do not need to prove Argon2 correctness.
6. **Add focused full-stack tests only for flows where cross-layer behavior is itself the risk.** Keep them few and meaningful.
7. **Treat production configuration as testable behavior.** Cookie/security/AWS profile assumptions should eventually have configuration-level tests.

## 11. Next testing focus: Assessment

The next vertical slice should add tests in roughly this order:

```text
Assessment Domain invariants
  ↓
Start/Resume/Restart Application tests
  ↓
Assessment repository + Flyway/Testcontainers integration
  ↓
Assessment Web MVC tests
  ↓
Submit + deterministic scoring tests
  ↓
focused concurrency/recovery tests
```

The Identity/Security tests established in this checkpoint become reusable infrastructure for authenticated Assessment endpoint testing.
