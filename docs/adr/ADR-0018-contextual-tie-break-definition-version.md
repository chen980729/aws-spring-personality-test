# ADR-0018 — Contextual Tie-break Requires a New DefinitionVersion

## Status

Accepted, 2026-10-05. Documentation-only decision; implementation, migration and publication of 1.1 remain pending.

## Context

Published `SIXTEEN_PERSONALITY` 1.0 binds Sessions to an immutable specification: 48 questionnaire questions, deterministic scoring/ambiguity, bounded clarification, and direct-pole exact-tie decisions recorded as `DimensionTieBreak` with source `USER_TIE_BREAK`.

The accepted contextual design asks one forced-choice scenario question per dimension when an exact questionnaire tie remains unresolved after `UNCLEAR` / `SKIPPED`. Even with an identical questionnaire and unchanged scores, that changes the evidence and finalization semantics. Applying it to published 1.0 would change the meaning of existing Sessions and historical decisions.

## Decision

Preserve 1.0 unchanged. Define 1.1 as the next immutable specification, reusing exactly the same 48 questions, ScoringPolicy, AmbiguityPolicy and clarification behavior/revision. Advance only FinalizationPolicy to revision v2 and add `TieBreakQuestionDefinition[]` to the AssessmentSpecification snapshot. Use the exact accepted EI/SN/TF/JP instruction, prompts and option text in [the aligned specification](../sixteen-personality-spec-aligned.md#128-accepted-contextual-questions--11-only).

Each definition contains questionId, dimension, instruction, prompt and exactly two options, each with optionId, text and resolvedPole. Version-aware loading is mandatory: published 1.0 / FinalizationPolicy v1 persisted JSONB may omit `tieBreakQuestions` entirely (or be represented as empty by newer code) and that must continue to mean legacy direct-pole semantics. FinalizationPolicy v2 requires contextual definitions. Validate existing referenced dimensions, exactly one question per dimension under v2, globally unique question IDs, exactly two options, question-local unique option IDs, two different dimension poles and membership of every resolved pole in the referenced DimensionDefinition. A mapper must not globally require the new field when reading retained 1.0 specifications.

Backend resolves stable selectedOptionId to PoleCode from the Session's bound immutable DefinitionVersion. The Frontend receives and displays only instruction, prompt and stable option IDs/text, never option-to-pole mappings. This keeps deterministic interpretation and validation authoritative and makes display-order changes harmless. These questions add categorical evidence; they do not change raw scores or 50/50 questionnaire evidence.

Retain `DimensionTieBreak` rather than introducing a competing TieBreakResponse model. Conceptual fields are sessionId, dimension, questionId, selectedOptionId, resolvedPole and decidedAt. Legacy IDs remain null, never fabricated; contextual IDs are required. Keep both sources: USER_TIE_BREAK for legacy direct-pole decisions and TIE_BREAK_QUESTION for 1.1 contextual decisions. Renaming historical sources would erase the distinction between evidence paths.

Add version-aware GET `/api/v1/assessment-sessions/{sessionId}/tie-breaks/{dimensionCode}` returning DIRECT_POLE_SELECTION or CONTEXTUAL_QUESTION. Preserve PUT at the same path with exclusive legacy `{ "selectedPole": "I" }` or contextual `{ "questionId": "TB-EI-1", "selectedOptionId": "TB-EI-02" }` bodies. Bound-version validation applies to both. OpenAPI info.version advances from 0.4.0 to 0.5.0; `/api/v1` is unchanged. See [REST contract](../backend/06-rest-api-contract.md#tie-break).

When the implementation/migration is published, retire 1.0 and make 1.1 the sole AVAILABLE version. Existing 1.0 Sessions never auto-upgrade and must remain completable with original semantics. RETIRED changes new-Session eligibility, not retained Session execution or historical interpretation. This ADR does not activate 1.1.

## Alternatives rejected

- Modify published 1.0 in place: violates immutable Session-bound semantics and makes old/new exact-tie facts indistinguishable.
- Change the questionnaire or scoring alongside the tie-break: unnecessary scope; the accepted change concerns only unresolved exact-tie finalization evidence.
- Resolve options in the Frontend or accept a contextual client-selected pole: exposes mappings and lets client behavior/display position control authoritative interpretation.
- Replace USER_TIE_BREAK globally: corrupts historical meaning and removes support needed to complete retained legacy Sessions.
- Add TieBreakResponse as a parallel domain fact: duplicates the existing DimensionTieBreak concept and fragments the ubiquitous language.

## Consequences

The same questionnaire retains reproducible scoring, while version identity explains different finalization evidence. Audit/history preserves legacy direct choices and new contextual selections distinctly. AI remains bounded and does not generate tie-break questions, rewrite evidence, or decide the final type.

Backend must support two semantics simultaneously even though only one version is AVAILABLE for new Sessions. Definition loading, request validation, finalization and persistence/recovery must dispatch from the Session binding. Frontend must handle both interaction variants without obtaining mappings. Nullable legacy IDs and contextual required IDs need future persistence/migration design, and both final source values must remain readable.

Future tests must cover both versions, retirement/resume, immutable question content, definition validation, DTO mapping exclusion, retries and concurrent finalization. Existing technical-failure/clarification behavior and Session consistency boundaries remain intact. See [planned testing](../backend/10-backend-testing-strategy.md#14-planned-definitionversion-11-acceptance-coverage-not-implemented).

No Java, TypeScript, Flyway migration or test changes are part of this decision. Historical checkpoint documents remain unchanged.
