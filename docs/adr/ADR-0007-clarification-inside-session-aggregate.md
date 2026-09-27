# ADR-0007: Keep DimensionClarification Inside the AssessmentSession Aggregate for MVP

- Status: Superseded by ADR-0016
- Date: 2026-09-15

## Context

Clarification eligibility and completion readiness depend directly on the session's initial result and unresolved ambiguous dimensions.

## Decision (historical)

`DimensionClarification` was initially modeled as an Entity inside the `AssessmentSession` Aggregate for MVP rather than a separate Aggregate Root.

## Consequences

This choice was reasonable before implementation because it made eligibility, one-clarification-per-dimension, skip/complete, and finalization easy to describe as one Aggregate consistency boundary.

Implementation Steps 4-6 then established `DimensionClarification` as separately persisted workflow state with its own repository and lifecycle, while Step 7 requires an external LLM call between two short database transactions. That implementation feedback triggered the reconsideration anticipated by this ADR.

See `ADR-0016-dimension-clarification-separate-aggregate.md` for the current decision.
