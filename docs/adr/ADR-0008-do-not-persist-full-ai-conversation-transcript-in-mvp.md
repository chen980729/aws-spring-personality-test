# ADR-0008: Do Not Persist Full AI Conversation Transcript in MVP

- Status: Accepted
- Date: 2026-09-15

## Context

AI clarification may involve multiple conversational turns and may contain sensitive personal descriptions. Exact mid-conversation resume is not an MVP requirement. Persisting a complete transcript would increase privacy exposure, retention obligations, and persistence complexity.

## Decision

Treat active AI conversation context as temporary/ephemeral runtime state. The full conversation transcript is not authoritative Assessment history and is not persisted in MVP.

After successful clarification, persist only the accepted `ClarificationResult`, required `AIProvenance`, and a necessary summary if the product uses one.

If temporary conversation context is lost before an accepted result is persisted, the persisted `AssessmentSession` and `InitialAssessmentResult` remain valid, and the affected dimension may restart clarification from its clarification-ready state.

`AIProvenance` records the effective successful execution associated with the accepted result, not the full retry/turn history.

## Consequences

### Benefits

- Better privacy and data minimization.
- Lower persistence and audit complexity.
- Clear separation between temporary LLM interaction and authoritative domain history.

### Costs

- Exact message-turn resume is not guaranteed.
- Browser/server/runtime-context loss may require restarting the current dimension clarification.
- Future complete AI audit requirements would require a new model such as `ClarificationAttempt[]` and a revised retention policy.
