# Flyway Schema & JPA Persistence Mapping

> **Status:** Accepted MVP Detailed Design — implementation-aligned through contextual tie-break persistence expansion
> **Last updated:** 2026-10-05

## 1. Migration layout

The applied migration history is authoritative and must not be renumbered:

```text
V1__create_identity_tables.sql
V2__create_spring_session_tables.sql
V3__create_assessment_tables.sql
V4__seed_sixteen_personality_v1.sql
V5__add_clarification_execution_token.sql
V6__add_tie_break_question_provenance.sql
V7__seed_sixteen_personality_v1_1_draft.sql
V8__activate_sixteen_personality_v1_1.sql
```

`V3` creates the Assessment tables together with the Assessment partial-unique/query indexes required by the initial schema. `V4` seeds immutable application-owned Sixteen Personality 1.0 reference data. `V5` adds `active_execution_token` to `assessment_dimension_clarifications` for stale external-result protection. `V6` expands `assessment_dimension_tie_breaks` with contextual question/option provenance. `V7` seeds the complete Sixteen Personality 1.1 specification as `DRAFT`; it deliberately leaves 1.0 `AVAILABLE` during the compatibility deployment. `V2` contains the PostgreSQL schema required by the project's pinned Spring Session JDBC version and is owned by Flyway rather than runtime auto-initialization.

V8 is the applied forward-only activation migration. It was released only after the compatible Backend/Frontend version had been deployed and old ECS tasks had drained; it retires 1.0 and promotes 1.1 to the sole `AVAILABLE` version. The migration retires 1.0 first to preserve the one-AVAILABLE-version unique index, assigns 1.1 its publication timestamp, and fails if the expected source states are not present. Applied migrations V1-V8 are immutable history and are never edited or renumbered. Future Group migrations begin after V8 rather than reusing an applied version number.

## 2. Naming convention

```text
table: plural_snake_case
column: snake_case
PK: pk_<table>
FK: fk_<table>_<meaning>
UNIQUE: uq_<meaning>
CHECK: ck_<meaning>
index: ix_<meaning>
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
- V3 partial unique index: at most one `AVAILABLE` row per `definition_id`.

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
active_execution_token
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
- `IN_PROGRESS` requires `active_execution_token`; every other status requires it to be NULL.
- retry replaces the active execution token for the same logical Clarification; accepted/failure/skip transitions clear it.

Dimension/pole semantics are intentionally not hardcoded in SQL.

## 7. Tie-break table

`assessment_dimension_tie_breaks`:

```text
session_id
dimension_code
question_id          NULL for legacy 1.0
selected_option_id   NULL for legacy 1.0
selected_pole        resolved business outcome
decided_at
PK(session_id, dimension_code)
```

`V6` adds `question_id` and `selected_option_id` without rewriting legacy rows. A CHECK constraint requires both provenance columns to be null together or populated together, and populated IDs may not be blank. The existing `selected_pole` column is intentionally retained: for 1.0 it is the directly selected pole, while for 1.1 it is the Backend-resolved pole derived from the immutable question/option mapping. This keeps finalization queries simple while retaining the contextual evidence path.

The database protects structural provenance consistency; Domain/Application validates whether the Session's bound DefinitionVersion permits legacy or contextual semantics, whether the dimension truly requires a tie-break, and whether the resolved pole belongs to that dimension.

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

## 11. V4 reference-data seed

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

Cross-Aggregate entities store scalar IDs. Do not create a bidirectional object graph merely because foreign keys exist.

`AssessmentSession` and `DimensionClarification` are separate Assessment Aggregate Roots (ADR-0016). Infrastructure therefore exposes Domain repository abstractions for each Aggregate where mutation/read behavior requires them. `DimensionClarification` references its parent Session by scalar `AssessmentSessionId`; it is not modeled as a cascading JPA `@OneToMany` collection on the Session Domain object.

A physical `ON DELETE CASCADE` from Session to Clarification/tie-break rows is allowed for hard-delete cleanup because those rows have no valid persistence lifecycle after their Session is deleted. That physical rule must not be interpreted as an in-memory Aggregate ownership rule.

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
