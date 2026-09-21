# ADR-0003: Use Deterministic Scoring with Optional AI Clarification

- Status: Accepted
- Date: 2026-09-15

## Context

The product needs a stable assessment foundation while using AI only to help users reflect on close/ambiguous dimensions.

## Decision

Questionnaire scoring and ambiguity evaluation are deterministic. AI clarification is optional and can only operate on dimensions already classified as ambiguous. AI cannot rewrite `InitialAssessmentResult` or control the final business workflow.

## Consequences

Core assessment behavior remains testable without LLM availability. AI is an enhancement rather than the source of truth, which improves explainability and resilience.
