# ADR-0004: Scope Clarification by Ambiguous Dimension

- Status: Accepted
- Date: 2026-09-15

## Context

An assessment may have multiple ambiguous dimensions, while clear dimensions should not be reopened by AI.

## Decision

Clarification is modeled per ambiguous dimension. One logical `DimensionClarification` exists per `Session + Dimension`. Session lifecycle does not create EI/JP-specific top-level states.

## Consequences

The model supports multiple ambiguous dimensions without state explosion and preserves a clear link between initial evidence and clarification outcome.
