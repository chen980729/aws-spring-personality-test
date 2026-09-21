# ADR-0005: Separate InitialAssessmentResult and FinalAssessmentResult

- Status: Accepted
- Date: 2026-09-15

## Context

Deterministic questionnaire evidence and the final accepted assessment conclusion have different meanings when optional clarification exists.

## Decision

Persist immutable `InitialAssessmentResult` separately from immutable `FinalAssessmentResult`. Clarification does not mutate initial evidence; final conclusions record their decision source.

## Consequences

The system can explain what the questionnaire originally indicated and how the final conclusion was reached without overwriting evidence.
