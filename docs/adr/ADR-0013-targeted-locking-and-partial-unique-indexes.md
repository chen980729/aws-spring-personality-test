# ADR-0013: Use Partial Unique Indexes, Optimistic Locking, and Targeted Row Locks

- Status: Accepted
- Date: 2026-09-20

## Context

Several invariants are conditional across rows/history (one AVAILABLE AssessmentDefinitionVersion, one active Assessment, one active Membership, one pending JoinRequest, one active Share), while others span distinct Aggregate rows (for example Admin transfer racing with Membership leave). Optimistic locking alone cannot protect concurrent INSERTs or all cross-Aggregate races.

## Decision

Keep PostgreSQL `READ COMMITTED` and combine:

- partial unique indexes for conditional uniqueness.
- JPA `@Version` for stale updates to an existing Aggregate root.
- selective `FOR UPDATE` root locks for known cross-Aggregate consistency hotspots.

For important Group mutations, lock the Group row as the serialization anchor. For clarification/finalization and Assessment sharing/deletion races, use short AssessmentSession root locks. Never hold locks across external LLM calls.

## Consequences

### Benefits

- Known races are explicitly protected without globally using SERIALIZABLE.
- Concurrency reasoning is testable and explainable.
- Historical rows remain compatible with “one currently active” invariants.

### Costs

- Important mutation paths need consistent lock ordering.
- Same-Group mutation concurrency is intentionally reduced.
- Focused PostgreSQL concurrency tests are required.
