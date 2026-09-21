# ADR-0011: Use Relational Lifecycle State with JSONB Assessment Snapshots

- Status: Accepted
- Date: 2026-09-20

## Context

Assessment persistence contains both lifecycle entities and small bounded immutable snapshots. Fully normalizing every questionnaire answer/result component would add many rows and mapping steps without improving current query needs, while putting every lifecycle concern in one JSON document would weaken constraints and concurrency control.

## Decision

Use relational tables/columns for identity, lifecycle, relationships and independently constrained child state. Store bounded immutable Assessment snapshots such as questionnaire response, initial/final result and executable definition specification as JSONB.

Do not add JSONB GIN indexes until an actual nested-field query requires them.

## Consequences

### Benefits

- Persistence matches access patterns and Aggregate semantics.
- Lifecycle constraints remain enforceable by PostgreSQL.
- Snapshot mapping remains compact.

### Costs

- JSON persistence schema requires deliberate mapping/version discipline.
- Ad-hoc SQL analytics over nested answer fields are less direct.
