# ADR-0007: Keep DimensionClarification Inside the AssessmentSession Aggregate for MVP

- Status: Accepted
- Date: 2026-09-15

## Context

Clarification eligibility and completion readiness depend directly on the session's initial result and unresolved ambiguous dimensions.

## Decision

`DimensionClarification` is an Entity inside the `AssessmentSession` Aggregate for MVP rather than a separate Aggregate Root.

## Consequences

Strong consistency for eligibility, one-clarification-per-dimension, skip/complete, and finalization is simpler. A future split may be reconsidered if clarification becomes long-lived, independently managed, or operationally heavy.
