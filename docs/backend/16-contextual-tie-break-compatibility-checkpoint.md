# Contextual Tie-break Compatibility — Step 2E Checkpoint

> **Status:** Implemented and pre-activation acceptance-tested
> **Date:** 2026-10-05
> **Definition state:** `SIXTEEN_PERSONALITY` 1.0 = `AVAILABLE`; 1.1 = `DRAFT`
> **API contract:** OpenAPI v0.5.0

## 1. Purpose

This checkpoint records the implemented Backend compatibility layer for the contextual exact-tie flow introduced by DefinitionVersion 1.1.

The goal is not merely to add a new question UI. The Backend must preserve historical 1.0 semantics while safely understanding a second immutable finalization policy before that version is activated for new Sessions.

The implementation therefore follows:

```text
expand persistence
    ↓
seed 1.1 as DRAFT
    ↓
deploy Backend that understands 1.0 + 1.1
    ↓
pre-activation acceptance sweep
    ↓
later activation migration
```

Activation and Frontend integration are deliberately outside this checkpoint.

## 2. Immutable specification model

DefinitionVersion 1.0 remains the legacy direct-pole specification.

DefinitionVersion 1.1 reuses exactly the same:

- four dimensions;
- 48 questionnaire questions;
- answer scale;
- question order and keyed-pole mapping;
- ScoringPolicy;
- AmbiguityPolicy;
- ClarificationPolicy.

Only finalization semantics advance:

```text
1.0
FinalizationPolicy revision = v1
tieBreakQuestions = absent / empty
exact tie -> direct pole selection

1.1
FinalizationPolicy revision = v2
tieBreakQuestions = EI / SN / TF / JP contextual definitions
exact tie -> stable option selection
```

The Domain now models stable `TieBreakQuestionId`, `TieBreakOptionId`, `TieBreakQuestionDefinition` and `TieBreakOptionDefinition` values. v2 validation requires exactly one valid contextual question per dimension and exactly two options resolving to the two different poles.

Legacy persisted 1.0 JSONB may omit `tieBreakQuestions` entirely and remains loadable.

## 3. Persistence expansion

Applied forward-only migrations in this feature line:

```text
V6__add_tie_break_question_provenance.sql
V7__seed_sixteen_personality_v1_1_draft.sql
```

V6 expands `assessment_dimension_tie_breaks`:

```text
session_id
dimension_code
question_id          nullable for legacy 1.0
selected_option_id   nullable for legacy 1.0
selected_pole        resolved business outcome
decided_at
```

The database requires `question_id` and `selected_option_id` to be both null or both populated.

V7 stores the complete 1.1 specification as `DRAFT`. It does **not** change availability:

```text
1.0 -> AVAILABLE
1.1 -> DRAFT
```

Previously published V4/V5 migrations are not modified.

## 4. Version-aware Application and REST boundary

The Session's own `definitionVersionId` is authoritative.

The Backend never selects semantics from:

- the request shape alone;
- whichever version is currently AVAILABLE;
- Frontend-supplied pole mappings.

GET:

```text
GET /api/v1/assessment-sessions/{sessionId}/tie-breaks/{dimensionCode}
```

returns:

```text
1.0 -> DIRECT_POLE_SELECTION
1.1 -> CONTEXTUAL_QUESTION
```

Contextual responses expose only:

- dimensionCode;
- questionId;
- instruction;
- prompt;
- optionId;
- option text.

They never expose `resolvedPole` or option-to-pole mappings.

PUT preserves the same resource path with exclusive request shapes:

```json
{ "selectedPole": "I" }
```

or:

```json
{
  "questionId": "TB-EI-1",
  "selectedOptionId": "TB-EI-02"
}
```

The Application resolves the contextual option against the immutable specification bound to that Session.

## 5. Persisted fact and finalization provenance

One Domain concept remains authoritative: `DimensionTieBreak`.

Legacy fact:

```text
questionId = null
selectedOptionId = null
resolvedPole = selected direct pole
```

Contextual fact:

```text
questionId = stable contextual question ID
selectedOptionId = stable selected option ID
resolvedPole = Backend-resolved pole
```

Finalization derives source from the fact itself:

```text
legacy provenance
    -> USER_TIE_BREAK

contextual provenance
    -> TIE_BREAK_QUESTION
```

The `AssessmentFinalizationService` therefore does not need to branch on a version string. Version-specific request validation happens earlier; finalization interprets the durable evidence path.

Contextual tie-breaks do not rewrite questionnaire evidence. An exact tie remains:

```text
rawScore = 0
questionnairePreference = null
display evidence = 50 / 50
```

## 6. Session/history projection

Legacy Session detail retains:

```text
dimensionCode
selectedPole
decidedAt
```

Contextual Session detail exposes:

```text
dimensionCode
questionId
selectedOptionId
decidedAt
```

The contextual public/read DTO does not expose the internally resolved pole as if the user had selected it directly.

Authoritative historical detail reloads this provenance from PostgreSQL rather than relying on transient command-response state.

## 7. Retry and conflict semantics

Equivalent retries are idempotent.

For an already accepted contextual decision:

```text
same questionId + same selectedOptionId
    -> return authoritative Session
    -> keep the same decidedAt
    -> do not insert another fact
```

A different selection for the same Session + dimension is immutable-history conflict:

```text
different accepted selection
    -> 409 TIE_BREAK_ALREADY_DECIDED
    -> persisted fact unchanged
```

Malformed/mixed request shapes remain 400; semantically invalid version/question/option selections remain 422.

## 8. Step 2E acceptance sweep

The pre-activation sweep verifies:

- 1.1's 48 questionnaire definitions exactly equal 1.0.
- Scoring/Ambiguity/Clarification policies remain equal.
- only FinalizationPolicy revision advances from v1 to v2.
- contextual wording and stable EI/SN/TF/JP IDs match the accepted specification.
- 1.0 JSONB without contextual definitions remains readable.
- v2 definition invariants reject malformed contextual content.
- 1.0 AVAILABLE Sessions still expose direct-pole semantics.
- a DRAFT 1.1 Session still executes contextual semantics because its bound version is authoritative.
- both SKIPPED and accepted UNCLEAR exact-tie paths require contextual interaction for 1.1.
- wrong-version, wrong-question and wrong-option submissions are rejected.
- contextual mapping is Backend-owned.
- raw score remains zero and evidence remains 50/50.
- completed contextual results use `TIE_BREAK_QUESTION`.
- final-result JSONB and historical workflow/detail preserve contextual provenance.
- same-value completed retry keeps the original `decidedAt`.
- conflicting retry cannot overwrite the fact.
- the final contextual tie-break racing with `skip-remaining` is serialized by the Session row lock and ends with exactly one immutable fact per dimension and one final result.

CI checkpoint:

```text
Backend Maven verify: 263 tests
Failures: 0
Errors:   0
Skipped:  0

Frontend:             success
```

The Docker image gate remains part of the repository CI pipeline and must also stay green before the feature PR is merged.

## 9. Why activation is still separate

This checkpoint intentionally does not add the activation migration.

During ECS rolling deployment, a new task can execute Flyway while old tasks are still serving requests. If the same migration immediately promoted 1.1, an old task could start a Session bound to semantics it does not understand.

The safe order is:

```text
1. deploy compatibility-capable Backend
2. verify old tasks have drained
3. apply forward-only activation migration
4. 1.0 -> RETIRED
5. 1.1 -> AVAILABLE
```

The activation step must then prove both sides of the transition:

```text
retained 1.0 Session
-> still completes with direct-pole / USER_TIE_BREAK

new Session after activation
-> binds 1.1
-> uses contextual question / TIE_BREAK_QUESTION
```

## 10. Remaining work

Not completed by this checkpoint:

- DefinitionVersion 1.1 activation migration.
- post-activation retained-1.0/new-1.1 binding verification.
- React contextual tie-break interaction.
- Frontend `FinalDecisionSource` support for `TIE_BREAK_QUESTION`.
- 8 pole descriptions and 16 personality-type descriptions.
- 16 personality hero/result image integration.
- provider-backed Step 8 LLM clarification runtime.

Historical Step 6/7 checkpoint documents are intentionally left unchanged.


## 11. Post-checkpoint progress — 2026-10-06

The Step 2E checkpoint above intentionally records the pre-activation Backend state as it existed on 2026-10-05. Subsequent Frontend work has now completed the previously listed browser-side compatibility items:

```text
F7-A -> dual-version TypeScript/API contract
F7-B -> contextual Tie-break interaction + retained 1.0 rollout compatibility
F7-C -> result interpretation/content + 16 result images
F7-D -> Landing hero integration
```

The Frontend now understands `TIE_BREAK_QUESTION`, renders 1.1 contextual questions without receiving option-to-pole mappings, and keeps the legacy 1.0 direct-pole flow operational during mixed-version deployment windows.

The remaining release boundary is therefore narrower than the historical Section 10 list:

```text
compatibility release deploy
        ↓
verify old ECS tasks drained
        ↓
separate activation migration/release
        ↓
1.0 RETIRED / 1.1 AVAILABLE
        ↓
post-activation retained-1.0 + new-1.1 production verification
```

Provider-backed Step 8 clarification remains a later product milestone and is unrelated to DefinitionVersion 1.1 activation.
