# Persistence / PostgreSQL Detailed Design

> **Status:** Accepted MVP Detailed Design — implementation-aligned through Assessment Step 6
> **Last updated:** 2026-09-28

## 1. Persistence stack

```text
PostgreSQL
+ Flyway SQL migrations
+ Spring Data JPA / Hibernate
+ separate Domain and Persistence models
+ constraints / partial unique indexes
+ optimistic locking
+ selective pessimistic row locking
```

Responsibility split:

```text
Domain      -> business meaning and invariants
Application -> workflow and transaction
Database    -> durable structural constraints and concurrency-sensitive uniqueness
```

MVP does not use database triggers as a hidden second Domain layer.

## 2. Schema and IDs

Use one PostgreSQL database and the `public` schema. Java modules, APIs and architecture tests provide module isolation; separate PostgreSQL schemas are not needed in the MVP.

Business entities use application-generated UUID v4 values stored as PostgreSQL `uuid`. Domain wraps them in typed IDs.

All business timestamps use Java `Instant` and PostgreSQL `timestamptz`.

## 3. Enum strategy

Use Java enums mapped to strings and PostgreSQL `varchar` columns protected by `CHECK` constraints rather than PostgreSQL native ENUM types.

This keeps Flyway evolution and JPA mapping simpler while still rejecting invalid persisted values.

## 4. Main tables

```text
user_accounts

assessment_definitions
assessment_definition_versions
assessment_sessions
assessment_dimension_clarifications
assessment_dimension_tie_breaks

groups
group_memberships
join_requests
group_assessment_shares
```

No MVP tables are added for domain events, outbox events, full AI transcripts or one-row-per-answer persistence.

## 5. Assessment persistence shape

### Assessment definition

`assessment_definitions` stores stable Assessment identity.

`assessment_definition_versions` stores an immutable executable specification snapshot, primarily as JSONB, after it becomes available/published.

For one `AssessmentDefinition`, at most one version may be `AVAILABLE` in MVP. A partial unique index on `definition_id` where `status = 'AVAILABLE'` makes `StartAssessment(assessmentCode)` resolve one unique executable version even under concurrent publication/update operations.

Sessions bind one exact version and never auto-upgrade.

### Session hybrid model

Use relational columns/tables for identity, lifecycle and independently constrained child lifecycle. Use JSONB for bounded immutable snapshots:

```text
questionnaire_response
initial_result
final_result
specification
```

This deliberately avoids exploding the 48-question questionnaire and small result objects into many relational rows when they are loaded/replaced as units.

`DimensionClarification` remains relational because it is a separate Assessment Aggregate with its own durable lifecycle/uniqueness semantics (ADR-0016). User tie-break facts are also relational because they have independent uniqueness/query semantics.

## 6. Generic Assessment schema rule

Generic Assessment tables do not hardcode Sixteen Personality-specific dimensions (`EI`, `SN`, `TF`, `JP`) or pole sets.

The authoritative `AssessmentDefinitionVersion` decides whether a dimension/pole is valid. This avoids a second source of truth in SQL and preserves support for future Assessment types.

## 7. Active Assessment uniqueness

Active Session states:

```text
IN_PROGRESS
AWAITING_CLARIFICATION
CLARIFICATION_IN_PROGRESS
```

Use a partial unique index on `(user_id, definition_id)` for these states. Application pre-checks provide normal UX; the database index protects races across concurrent requests/processes.

`StartAssessment` handles a uniqueness race by reloading the existing active Session rather than surfacing an internal server error.

`RestartAssessmentSession` first locks the identified active Session, abandons it, and creates the replacement in one transaction. Because the command targets the old `sessionId`, a retry after success cannot accidentally abandon the new replacement Session.

## 8. Durable questionnaire submission

`questionnaire_submitted_at` is a durable irreversible fact rather than a boolean or a dedicated Session status.

MVP submission is atomic with all deterministic post-submission processing. Therefore a committed Session does **not** use a `submitted-but-not-scored` recovery state:

```text
IN_PROGRESS
-> questionnaire_submitted_at IS NULL
-> initial_result IS NULL

AWAITING_CLARIFICATION / CLARIFICATION_IN_PROGRESS / COMPLETED
-> questionnaire_submitted_at IS NOT NULL
-> initial_result IS NOT NULL

ABANDONED
-> may be pre-submit (both NULL) or post-submit (both NOT NULL)
```

If the Submit transaction fails before commit, all submission/scoring state rolls back and submission did not happen.

## 9. Clarification persistence

Each Session/dimension has at most one logical `DimensionClarification` row.

Accepted clarification business data persists:

- resolution (`RESOLVED` / `UNCLEAR`).
- suggested pole.
- confidence.
- optional summary.
- provider/model identifier.
- clarification policy revision.

Full multi-turn AI conversation is not persisted in MVP.

A partial unique index permits at most one `IN_PROGRESS` clarification per Session.

## 10. Assessment deletion

Historical Assessment uses **hard delete** in MVP.

The deletion workflow first ends all active Group shares referencing the Session, then deletes the Session. Session-dependent clarification/tie-break rows are physically deleted with it. A database cascade here expresses lifecycle cleanup, not that `DimensionClarification` belongs to the same DDD Aggregate.

Group share history keeps the old `assessment_session_id` as an opaque historical identity.

Therefore `group_assessment_shares.assessment_session_id` intentionally has **no database foreign key** to `assessment_sessions`:

- `RESTRICT` would block privacy deletion.
- `CASCADE` would destroy consent history.
- `SET NULL` would erase the historical reference identity.

Cross-domain referential consistency is protected by orchestration, local transaction and locking instead.

## 11. Group lifecycle persistence

`groups` retains Group identity and lifecycle (`ACTIVE` / `DISBANDED`). Disband is not a hard delete.

`group_memberships` retains historical membership rows. Multiple historical memberships are allowed, but only one ACTIVE membership for `(group_id, user_id)` via partial unique index.

`join_requests` retains request history. Multiple historical requests are allowed, but only one PENDING request for `(group_id, user_id)`.

`group_assessment_shares` retains sharing-consent history, with at most one ACTIVE Share per Membership.

## 12. GroupCode

MVP uses a 10-character uppercase Crockford-style Base32 identifier excluding ambiguous letters. Example:

```text
7K3M9X2QPA
```

Backend generates it with `SecureRandom`; database uniqueness is authoritative. GroupCode is a discovery identifier, never an authentication secret.

## 13. Cooldown / expiration facts

JoinRequest persists both `requested_at` and `expires_at`. The currently chosen 7-day policy is applied when a request is created; SQL only requires `expires_at > requested_at` rather than hardcoding exactly 7 days, preserving historical policy facts if the rule later changes.

The 8-hour rejection cooldown is calculated from rejection history; `next_allowed_request_at` is not persisted as a second derived fact.

Expired-but-still-PENDING rows may exist briefly if scheduling is late. Business operations must evaluate `expires_at`, normalize to `REJECTED/TIMED_OUT` when required, then continue.

## 14. Foreign-key policy

Use foreign keys for stable ownership/references inside the same persistent domain boundary and to `user_accounts`.

Default delete policy is `RESTRICT` for retained business history.

Use `ON DELETE CASCADE` only where the referenced row has no valid lifecycle after its parent is deleted. For Assessment hard deletion this includes Clarification/tie-break rows; the cascade is a physical lifecycle rule and does not redefine the ADR-0016 Aggregate boundary.

Database cascade must not replace explicit lifecycle operations such as Group disband.

## 15. Optimistic locking

Mutable roots use JPA `@Version` / `bigint version`:

- UserAccount.
- AssessmentSession.
- Group.
- GroupMembership.
- JoinRequest.
- GroupAssessmentShare.

This protects stale updates to an existing row (e.g. autosave vs submission).

Optimistic locking cannot protect two concurrent INSERTs that collectively violate “one ACTIVE/PENDING row”; partial unique indexes handle those invariants.

## 16. Group concurrency anchor

Some Group invariants span different Aggregate rows. Example: `TransferAdmin` racing with the new Admin's `LeaveGroup` could otherwise leave an ACTIVE Group whose Admin Membership has ended.

MVP therefore serializes important mutations for the same Group by locking the Group row (`SELECT ... FOR UPDATE`) before mutating Membership/JoinRequest/Share state.

This favors correctness and explainability over maximum write parallelism for small collaborative Groups.

Recommended lock order:

```text
Group-only:
1 Group
2 Membership
3 JoinRequest
4 Share

Cross Assessment/Group:
1 AssessmentSession
2 Group
3 Membership
4 Share
```

## 17. Assessment concurrency anchor

Assessment draft autosave mainly relies on optimistic locking.

Clarification, skip, tie-break and finalization phases use short AssessmentSession root locks so separate Clarification Aggregate mutations cannot independently conclude that another unresolved dimension still exists and thereby miss finalization.

Locks are never held across LLM network calls.

## 18. Share vs Assessment-delete race

Both sharing and historical deletion lock the AssessmentSession first.

- If sharing commits first, deletion sees and ends the new active Share before deleting.
- If deletion commits first, sharing wakes and finds that the Session no longer exists.

Therefore no active Share can survive a committed Assessment deletion.

## 19. Isolation level

Keep PostgreSQL default `READ COMMITTED`; do not globally enable `SERIALIZABLE`.

Use explicit partial uniqueness, `@Version`, and targeted row locks for known conflicts.

## 20. Index philosophy

Create indexes because they enforce an invariant or support a known query path. Avoid speculative collections of indexes.

JSONB receives no GIN index initially because JSON is primarily loaded by parent PK rather than queried by nested fields.

## 21. Deferred account deletion impact

Account deletion is outside MVP. The current schema intentionally uses `ON DELETE RESTRICT` for User references and therefore does **not** support direct `DELETE FROM user_accounts`.

A future account-deletion capability must define one coordinated policy for:

- owned AssessmentSessions and private assessment content.
- Groups created by the user.
- current Group Admin authority (transfer/disband before account removal).
- Membership history.
- JoinRequest history.
- Share/consent history.
- deletion versus anonymization of historical identity references.

This is deliberate deferred design, not an accidental FK limitation.

## 22. Migration ownership

Flyway owns production schema evolution. Hibernate uses:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Never use `create`/`update` as the authoritative production schema process.

Versioned migrations become immutable after entering shared history; corrections roll forward with a new migration.
