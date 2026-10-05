# Backend Testing Strategy & Current Coverage

> **DefinitionVersion 1.1 status (ADR-0018):** Specification Domain, persistence expansion, and version-aware Tie-break Application/REST are implemented. 1.1 remains DRAFT while finalization compatibility and activation are pending. Published 1.0 remains immutable and supported.

> **Status:** Active implementation guidance
> **Last reviewed:** 2026-10-05
> **Current coverage checkpoint:** Step 7 baseline + DefinitionVersion 1.1 compatibility through Step 2C

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

## 11. Assessment coverage baseline through Step 6

The Assessment implementation now follows the intended layered strategy.

### 11.1 Domain / pure logic

Current focused coverage includes:

```text
AssessmentSession lifecycle invariants
QuestionnaireResponse invariants
deterministic questionnaire scoring
ambiguity threshold behavior
exact-tie behavior
deterministic immediate finalization
submission freeze / terminal-state rules
abandonment rules
```

### 11.2 Application

Focused Application tests cover:

```text
Start / Resume
Session-bound questionnaire read
Autosave
SubmitQuestionnaire
GetAssessmentSession
RestartAssessmentSession
ListAssessmentHistory
Get historical AssessmentSession detail
```

Ports are mocked at this layer rather than mocking internal Domain behavior.

### 11.3 PostgreSQL / persistence integration

PostgreSQL Testcontainers are used for database-specific behavior including:

```text
Flyway schema validation
Definition / Version seed loading
JSONB round trips
partial UNIQUE one-active-session invariant
atomic active-session creation
optimistic locking for draft writes
submission persistence
InitialAssessmentResult / FinalAssessmentResult persistence
PENDING clarification creation
Restart row locking and replacement persistence ordering
concurrent Restart against the same old Session
completed-only History projection and pagination
owner-isolated History reads
authoritative persisted Clarification/Tie-break workflow reads for Session detail
```

The Restart concurrency acceptance criterion is:

```text
two concurrent Restart commands
against the same old sessionId

-> exactly one replacement succeeds
-> the other observes the old Session as ABANDONED/conflicts
-> exactly one active replacement remains
```

This is intentionally verified against real PostgreSQL rather than H2.

### 11.4 Web MVC / Security

Focused `@WebMvcTest` coverage now includes:

```text
Assessment catalog/read endpoints
Start / Resume
active Session read
Session-bound questionnaire read
Autosave
Submission
Session detail
Restart
History with default/explicit pagination
historical Session detail
authentication
CSRF
privacy-safe 404 behavior
stable 409 / 422 Problem Details
Location header on created resources
```

### 11.5 Runtime verification

Important flows are additionally exercised manually through the running application where the risk is cross-layer behavior rather than one isolated class.

Verified milestones include:

```text
register/login/session/CSRF
start/resume
autosave/read-back
submit/read-back
submission retry conflict
restart
restart retry recovery
active successor recovery
completed History paging
historical detail read-back
```

## 12. Step 7 Clarification / Tie-break coverage

Step 7 now has automated coverage across Domain, Application, PostgreSQL and Web MVC boundaries for the existing legacy 1.0 behavior. This coverage does not establish implementation of 1.1 contextual questions.

Verified behavior includes:

```text
DimensionClarification
PENDING -> IN_PROGRESS
IN_PROGRESS -> CLARIFIED (RESOLVED / UNCLEAR)
IN_PROGRESS -> FAILED_RETRYABLE
FAILED_RETRYABLE -> IN_PROGRESS retry
PENDING / IN_PROGRESS / FAILED_RETRYABLE -> SKIPPED

accepted ClarificationResult / AIProvenance persistence round-trip
one logical Clarification per Session + Dimension
one IN_PROGRESS Clarification per Session at the database boundary

Skip one / Skip remaining
non-zero questionnaire fallback
exact-tie detection
explicit persisted user tie-break
USER_TIE_BREAK final decision source
automatic deterministic finalization once every dimension is resolvable

execution token persisted only while IN_PROGRESS
retry creates a new execution token
late completion with an old token is discarded
late completion after Restart / abandonment is discarded

HTTP + authentication + CSRF + 404 privacy boundary
422 business-semantic errors for invalid Clarification / tie-break actions
```

The execution-boundary integration tests prove stale-result rejection and Restart recovery with real PostgreSQL. They do **not** yet constitute a dedicated two-thread/two-transaction stress test for every possible Clarification/finalization race.

## 13. Deferred backend-feature testing focus: Step 8 LLM Integration + concurrency hardening

The current project focus has moved to AWS/CI-CD, so the immediate testing additions should center on deployable configuration, build/startup verification and production-profile assumptions. When backend feature work returns to Step 8, it should add focused tests around the provider boundary without re-testing the deterministic Domain rules already covered above.

Priority areas:

```text
provider timeout / transport failure -> failRetryable
malformed structured output -> failRetryable
valid provider output -> accepted ClarificationResult
actual provider/model/policy provenance captured by Backend, not trusted from model output
no DB transaction / row lock held across provider call
temporary runtime context loss does not damage durable Assessment state
Start / Continue / Retry public HTTP interaction behavior
privacy/safety filtering around clarification context
```

Separately, a small set of explicit simultaneous-transaction tests remains valuable for:

```text
concurrent Start Clarification attempts
Skip vs provider completion
Retry vs late previous completion
final tie-break vs another finalization-triggering command
```

Because ADR-0016 makes `DimensionClarification` a separate Aggregate, these tests should keep proving Session + Clarification coordination and PostgreSQL constraints rather than relying only on mocked Application tests.

The Identity/Security test infrastructure remains reusable for authenticated Assessment and future Group endpoint testing.

Before production deployment is considered stable, add configuration-level verification for environment-driven datasource settings, secure production Session-cookie behavior, health/readiness expectations and any AWS-specific profile assumptions. CI should also run the existing backend suite together with the frontend test/build/lint gates rather than treating either build unit as optional.

## 14. DefinitionVersion 1.1 acceptance coverage

Coverage is being implemented incrementally. Steps 2A-2C now exercise specification invariants, legacy JSON compatibility, staged Flyway persistence, version-aware interaction/query behavior, contextual submission/persistence, request-shape validation, and legacy compatibility. Step 2D finalization-source behavior and the final Step 2E acceptance sweep remain pending.

### Immutable specification and content

- Compare 1.1's 48 questionnaire definitions with 1.0 exactly, including IDs, order, wording and keying; scoring, ambiguity and clarification policy behavior/revisions remain unchanged. Verify only finalization advances to v2 with contextual definitions.
- Verify the shared instruction and EI/SN/TF/JP prompt and option texts exactly match the accepted aligned specification §12.8, including stable question/option IDs.
- Reject nonexistent dimensions, missing/duplicate per-dimension questions under v2, duplicate question IDs, option counts other than two, duplicate option IDs within a question, same-pole option pairs, and poles outside the referenced DimensionDefinition.
- Published 1.0 specification remains byte/semantically unchanged and has no contextual questions; publication of 1.1 retires 1.0 and leaves exactly one AVAILABLE version without rewriting existing Session bindings.
- Load the real retained 1.0 JSONB shape where `tieBreakQuestions` is absent and verify it still maps successfully to legacy FinalizationPolicy v1 semantics. Newer in-memory representation may normalize it to an empty collection, but must never require/backfill contextual definitions. Conversely, reject FinalizationPolicy v2 specifications with missing/empty/incomplete contextual definitions.

### Domain and Application version matrix

- For each dimension, exact tie + UNCLEAR and exact tie + SKIPPED require the bound interaction: direct pole / USER_TIE_BREAK for 1.0, contextual option / TIE_BREAK_QUESTION for 1.1.
- RESOLVED clarification needs no tie-break; non-zero fallback and technical failure / Retry / explicit Skip remain unchanged.
- Resolve stable option IDs through the Session-bound version; option display order and the current AVAILABLE version must not change resolution. Reject a question from another dimension/version, an option from another question, wrong-version payloads and client-supplied contextual poles.
- Verify unchanged raw score and 50/50 evidence, one final conclusion per dimension, both retained sources, and deterministic type composition.

### Persistence, REST and recovery

- Round-trip evolved DimensionTieBreak fields: sessionId, dimension, nullable legacy questionId/selectedOptionId, resolvedPole, decidedAt. Never fabricate IDs for legacy facts; require genuine IDs for contextual facts.
- [partially implemented] 1.0 remains AVAILABLE and legacy direct-pole behavior remains covered while 1.1 is DRAFT. Retired-1.0 resume plus new-1.1 binding is deferred until the later activation migration and Step 2E acceptance sweep.
- [implemented through Step 2C] GET returns the correct interaction discriminator; contextual options expose only optionId/text, never resolvedPole or mappings. Session projection uses legacy selectedPole or contextual questionId/selectedOptionId shape.
- [implemented through Step 2C] PUT validates exclusive request shapes (including mixed/extra-field rejection), preserves authentication/ownership/CSRF/privacy boundaries, and distinguishes 400 malformed shape, 422 semantic rejection and 409 conflicting immutable decision.
- Equivalent PUT retries, including recovery after completion, preserve the original fact/time; conflicting re-selection is rejected.
- GET is read-only and returns an interaction only while that exact tie still requires user input. After a decision has been accepted, clients recover from the authoritative Session/tieBreak/finalResult projection rather than re-fetching the interaction; completed or already-resolved dimensions are not treated as GET-interaction recovery cases.
- Race the last tie-break against Skip/finalization using the Session lock boundary; produce one immutable fact per Session/dimension and one final result. Existing abandonment/stale-provider-result protections remain valid.

These are implementation acceptance gates to execute when feature work begins, not a reason to rewrite historical Step 6/7 checkpoint documents.
