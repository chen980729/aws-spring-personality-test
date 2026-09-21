# Flyway Schema & JPA Persistence Mapping

> **Status:** Accepted MVP Detailed Design — Final Alignment Applied  
> **Last updated:** 2026-09-21

## 1. Migration layout

Recommended initial migrations:

```text
V1__create_identity_tables.sql
V2__create_spring_session_tables.sql
V3__create_assessment_tables.sql
V4__create_group_tables.sql
V5__create_domain_indexes.sql
V6__seed_sixteen_personality_v1.sql
```

PK/FK/CHECK/ordinary UNIQUE constraints stay near table creation. Partial unique and query-oriented indexes are grouped in V5. V6 seeds immutable application-owned reference data. `V2` contains the PostgreSQL schema required by the project's pinned Spring Session JDBC version and is owned by Flyway rather than runtime auto-initialization.

## 2. Naming convention

```text
table: plural_snake_case
column: snake_case
PK: pk_<table>
FK: fk_<table>_<meaning>
UNIQUE: uq_<meaning>
CHECK: ck_<meaning>
index: idx_<meaning>
```

Constraint names are part of operational diagnostics and may be used to translate expected concurrency violations into stable application conflicts.

## 3. Identity table

`user_accounts` contains:

```text
id uuid PK
email text UNIQUE NOT NULL
display_name text NOT NULL
password_hash text NOT NULL
created_at timestamptz NOT NULL
version bigint NOT NULL
```

Email canonicalization and password-hashing policy remain Security concerns; DB guarantees the stored canonical email value is unique. `display_name` is the non-sensitive human-readable identity exposed in Group read models; email is not used as a Group display field.

## 3.1 Spring Session JDBC tables

`V2__create_spring_session_tables.sql` creates the Spring Session JDBC tables required by the pinned framework version. These tables are **technical infrastructure**, not Identity Domain tables.

Production configuration must disable Spring Session schema auto-initialization, conceptually:

```text
spring.session.jdbc.initialize-schema=never
```

The exact table/index DDL should track the official PostgreSQL schema shipped for the selected Spring Session version, with Flyway as the sole production schema owner.

## 4. AssessmentDefinition tables

`assessment_definitions`:

```text
id
code UNIQUE
name
created_at
```

`assessment_definition_versions`:

```text
id
definition_id FK
version_code
status
specification jsonb
created_at
published_at
```

Constraints include:

- unique `(definition_id, version_code)`.
- unique `(id, definition_id)` to support composite Session FK.
- status in `DRAFT/AVAILABLE/RETIRED`.
- specification must be a JSON object.
- DRAFT has no `published_at`; AVAILABLE/RETIRED do.
- V5 partial unique index: at most one `AVAILABLE` row per `definition_id`.

## 5. AssessmentSession table

Core columns:

```text
id
user_id
definition_id
definition_version_id
status
questionnaire_response jsonb
questionnaire_submitted_at
initial_result jsonb
final_result jsonb
created_at
completed_at
abandoned_at
version
```

Composite FK:

```text
(definition_version_id, definition_id)
 -> assessment_definition_versions(id, definition_id)
```

This prevents binding a Session's `definition_id` to a version belonging to a different definition.

Key state constraints:

- `IN_PROGRESS` is pre-submission: `questionnaire_submitted_at` and `initial_result` are NULL.
- submission fact and `initial_result` appear together after the atomic Submit transaction.
- clarification states require submitted questionnaire and initial result.
- COMPLETED requires submitted questionnaire, initial result, final result and `completed_at`.
- ABANDONED requires `abandoned_at` and no final result; an abandoned attempt may be either pre-submit or post-submit, but may not contain a half-submitted state.

## 6. Clarification table

`assessment_dimension_clarifications` stores:

```text
id
session_id
dimension_code
status
result_outcome
suggested_pole
result_confidence
result_summary
ai_provider
ai_model_identifier
clarification_policy_revision
started_at
accepted_at
updated_at
```

Constraints:

- unique `(session_id, dimension_code)`.
- status is one of `PENDING/IN_PROGRESS/CLARIFIED/SKIPPED/FAILED_RETRYABLE`.
- result outcome is `RESOLVED/UNCLEAR` when present.
- `CLARIFIED` requires accepted result/provenance fields.
- non-CLARIFIED states do not retain accepted result/provenance fields.

Dimension/pole semantics are intentionally not hardcoded in SQL.

## 7. Tie-break table

`assessment_dimension_tie_breaks`:

```text
session_id
dimension_code
selected_pole
decided_at
PK(session_id, dimension_code)
```

It records the user fact separately from the derived final result. Domain validates whether the dimension truly requires a tie-break and whether the pole is valid for the bound definition.

## 8. Group tables

`groups`:

```text
id
name
group_code UNIQUE
created_by_user_id FK
admin_user_id FK
status
created_at
disbanded_at
version
```

`group_memberships`:

```text
id
group_id FK
user_id FK
status
joined_at
ended_at
end_reason
version
```

`join_requests`:

```text
id
group_id FK
user_id FK
status
requested_at
expires_at
resolved_at
resolved_by_user_id FK
rejection_reason
version
```

`group_assessment_shares`:

```text
id
membership_id FK
assessment_session_id   -- intentionally NO FK
status
shared_at
ended_at
end_reason
version
```

CHECK constraints keep lifecycle timestamps and end/rejection reasons consistent with status.

## 9. Partial unique indexes

Required invariant indexes:

```sql
-- at most one executable/available version per AssessmentDefinition
ON assessment_definition_versions(definition_id)
WHERE status = 'AVAILABLE'

-- one active Assessment per user + definition
ON assessment_sessions(user_id, definition_id)
WHERE status IN ('IN_PROGRESS','AWAITING_CLARIFICATION','CLARIFICATION_IN_PROGRESS')

-- at most one clarification actively running per Session
ON assessment_dimension_clarifications(session_id)
WHERE status = 'IN_PROGRESS'

-- one active Membership per Group/User
ON group_memberships(group_id, user_id)
WHERE status = 'ACTIVE'

-- one pending JoinRequest per Group/User
ON join_requests(group_id, user_id)
WHERE status = 'PENDING'

-- one active Share per Membership
ON group_assessment_shares(membership_id)
WHERE status = 'ACTIVE'
```

These are consistency mechanisms first and indexes second.

## 10. Initial query indexes

Initial access-path indexes include:

- completed Assessment history by `(user_id, completed_at DESC)` with completed predicate.
- active Group memberships by user.
- JoinRequest history by user/requested time.
- rejected JoinRequest history for cooldown lookup.
- active Shares by `assessment_session_id` for Assessment deletion.

Do not add redundant indexes already served by PK/UNIQUE prefix ordering without an actual query reason.

## 11. V6 reference-data seed

Seed `SIXTEEN_PERSONALITY` AssessmentDefinition and immutable version `1.0` using fixed UUIDs and fixed publication timestamps.

The complete executable specification is stored as deterministic JSONB reference data. Avoid `now()` and runtime “if missing, insert” bootstrapping for this versioned business reference data.

## 12. JpaEntity rules

JPA classes live under infrastructure persistence and use raw persistence representations:

```java
@Id
@Column(name = "id")
UUID id;

@Version
@Column(name = "version")
long version;
```

Application-generated UUIDs mean no `@GeneratedValue`.

Enum status values use `EnumType.STRING` and are paired with DB CHECK constraints.

## 13. JSONB mapping

Infrastructure maps JSONB as a persistence representation (e.g. Hibernate JSON JDBC type / `JsonNode`) rather than directly persisting Domain value-object Java class structure.

A persistence JSON mapper converts:

```text
JsonNode <-> AssessmentSpecification
JsonNode <-> QuestionnaireResponse
JsonNode <-> InitialAssessmentResult
JsonNode <-> FinalAssessmentResult
```

This prevents a Domain Java refactor from silently redefining the database JSON contract.

## 14. JPA relationship policy

Cross-Aggregate entities store scalar IDs. Do not create a bidirectional Group object graph merely because foreign keys exist.

AssessmentSession-owned child rows may be loaded explicitly by the Session repository adapter rather than immediately introducing cascading `@OneToMany` graphs.

The physical child Spring Data repositories are infrastructure details; Application still sees one `AssessmentSessionRepository` Aggregate boundary.

## 15. Lock methods

Infrastructure may use JPA pessimistic write locking (`FOR UPDATE`) through repository methods such as conceptual `findByIdForUpdate`.

Application/Orchestration calls business/module contracts; it does not manipulate `EntityManager`, `LockModeType` or Spring Data repositories directly.

## 16. Persistence tests

Use real PostgreSQL through Testcontainers because the design depends on:

- partial indexes.
- JSONB.
- `timestamptz`.
- actual constraint behavior.
- row locking.

Do not use H2 as the authoritative persistence integration environment.

Required coverage includes:

- all migrations from an empty database.
- Hibernate schema validation.
- each partial uniqueness invariant, including the one-AVAILABLE-version rule.
- invalid lifecycle CHECK rejection.
- Definition/Version mismatch rejection.
- JSONB Domain round trips.
- Assessment hard delete child cascade.
- retained Group history.
- selected concurrency races (StartAssessment, RestartAssessment retry/replacement, JoinRequest, transfer-vs-leave, approve-vs-reject, clarification concurrency, share-vs-delete).

## 17. Database errors

Expected constraint races are translated to business/application outcomes; raw SQL/Hibernate exceptions never leak to HTTP.

The Application still pre-validates normal flows. Database constraints remain the final concurrency safety net, not the primary control-flow mechanism.
