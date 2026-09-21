# Spring Dependency & Layer Interaction Design

> **Status:** Accepted MVP Detailed Design  
> **Last updated:** 2026-09-20

## 1. Dependency model

```text
HTTP Request
  ↓
Controller
  ↓
Application Service
  ↓
Domain Model
  ↓
Repository / Output Port abstraction
  ↑
Infrastructure Adapter
  ↓
Spring Data JPA / external system
```

The Domain layer remains plain Java.

## 2. Domain model and JPA model

Formal decision:

```text
Domain Model != JPA Persistence Model
```

Example:

```text
Group                <-> GroupJpaEntity
GroupMembership      <-> GroupMembershipJpaEntity
AssessmentSession    <-> AssessmentSessionJpaEntity
```

This separation is chosen because the current Domain is no longer simple CRUD: Assessment contains immutable specification binding, submission/finalization boundaries and clarification lifecycle; Group contains multiple independent Aggregate Roots and retained history.

The project accepts mapping boilerplate in exchange for:

- no Hibernate annotations inside Domain.
- clear Aggregate boundaries.
- persistence schema evolution independent from Domain refactors.
- straightforward Domain unit tests.
- less temptation to let JPA relationships define business ownership.

## 3. Spring annotation ownership

| Annotation / technology | Layer |
|---|---|
| `@RestController`, request mapping, Bean Validation | Web |
| `@Service` | Application |
| `@Transactional` | Application / Orchestration |
| `@Repository` | Infrastructure |
| `@Entity`, `@Table`, `@Version` | Infrastructure persistence |
| `@Configuration`, `@Bean` | Infrastructure / Platform configuration |

Domain classes do not use Spring/JPA/Jackson annotations as a default design rule.

## 4. Controller responsibility

Controllers:

- parse HTTP/path/query/body.
- perform transport-level validation.
- extract authenticated principal.
- construct Application commands/query inputs.
- map Application outputs to HTTP response DTOs.

Controllers do **not**:

- access repositories.
- perform admin/ownership checks.
- score assessments.
- decide lifecycle transitions.
- start business transactions.

## 5. Validation ownership

Transport validation:

- missing field.
- blank string.
- malformed email/UUID/JSON.
- simple field-size constraints.

Application validation:

- resource existence.
- resource visibility.
- actor/resource relationship.

Domain validation:

- legal state transitions.
- invariant enforcement.
- Assessment/Group business rules.

Bean Validation is not a replacement for Domain validation.

## 6. Constructor injection

Application and infrastructure components use constructor injection. Field injection is avoided so dependencies remain explicit and easy to test.

## 7. Transaction boundary

`@Transactional` follows the complete business use case, not individual repository calls.

Example:

```text
ApproveJoinRequest transaction
= load/lock
+ authorize
+ approve request
+ create membership
+ persist both
```

Mutation transactions stay short. External network I/O, especially LLM calls, never holds database transactions open.

## 8. Domain Repository vs Spring Data Repository

Domain-facing example:

```text
group.domain.repository.GroupRepository
```

Infrastructure-facing example:

```text
group.infrastructure.persistence.repository.SpringDataGroupRepository
```

Adapter:

```text
JpaGroupRepositoryAdapter implements GroupRepository
```

Domain Repository interfaces do not extend `JpaRepository`, `CrudRepository` or `PagingAndSortingRepository`.

## 9. Query persistence path

Commands and business mutations reconstruct Domain Aggregates.

Queries may use dedicated ports/projections:

```text
Controller
 -> Query Service
 -> Query Port
 -> JPA/SQL projection
 -> Read Model
```

This is a light write/read separation, not a second database or full CQRS architecture.

## 10. JPA association policy

Database relationships do not automatically become Java object graphs.

Across Aggregate boundaries, prefer scalar IDs:

```java
UUID groupId;
UUID userId;
UUID assessmentSessionId;
```

rather than default `@ManyToOne` / `@OneToMany` graphs.

Specifically, Group, Membership, JoinRequest and Share remain separate Aggregate Roots even though their tables have foreign keys.

## 11. Persistence mapper policy

Mapping belongs under `infrastructure.persistence.mapper`.

MVP starts with manual mapping so the Domain/Persistence differences remain visible and debuggable. MapStruct may be considered later if repetition becomes substantial.

## 12. Value-object IDs

Domain/public module contracts wrap UUIDs in types such as:

```text
UserId
GroupId
MembershipId
JoinRequestId
AssessmentSessionId
```

Persistence converts them to/from raw PostgreSQL UUID values.

## 13. Time handling

Inject `Clock` rather than calling `Instant.now()` throughout business logic.

```java
@Bean
Clock clock() {
    return Clock.systemUTC();
}
```

Application obtains `Instant now = clock.instant()` and passes time to Domain behavior.

This is required for deterministic tests of:

- JoinRequest 7-day expiration.
- 8-hour retry cooldown.
- lifecycle timestamps.

Persisted timestamps use `Instant` / PostgreSQL `timestamptz`; UI localizes them later.

## 14. Error flow

Domain/Application errors contain business meaning, not HTTP status codes.

Web uses centralized `@RestControllerAdvice` to map them into the public error contract. Controllers do not contain repeated `try/catch` response construction.

## 15. Domain services / policies

Pure business policies such as Scoring, Ambiguity and Finalization remain plain Java. They do not need Spring stereotypes merely because Application calls them.

## 16. Identity and AI inversion

Examples of outbound abstractions:

```text
PasswordHasher
ClarificationAssistant
```

Infrastructure implements them with Spring Security / provider SDKs. Domain/Application code does not import BCrypt/OpenAI/provider-specific DTOs directly.

## 17. Testing consequences

The design naturally separates:

- Domain unit tests — no Spring/DB.
- Application tests — fake/mock ports/repositories.
- Persistence integration tests — real PostgreSQL/Testcontainers.
- API integration tests — Spring MVC/Security/serialization/application integration.

## 18. Core rule

```text
Spring wires the system.
Domain defines business truth.
JPA persists it but does not define it.
```
