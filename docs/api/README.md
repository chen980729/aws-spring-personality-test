# API Contract

> **Status:** OpenAPI v0.6.0 — active accepted design contract
> **Last updated:** 2026-10-14
> **Implementation alignment:** Step 8G-A implements the OpenAPI v0.6.0 clarification HTTP boundary (Start / Continue / Retry / read-only Current Turn) on the Backend. React interaction remains Step 8G-B; Bedrock stays disabled by default until AWS enablement in Step 8H/8I. This branch is not yet claimed as deployed.

`openapi.yaml` is the machine-readable HTTP contract for the Backend MVP.

The split of responsibilities is:

```text
docs/backend/06-rest-api-contract.md
    -> REST resource/action semantics and rationale

docs/backend/08-authentication-security.md
    -> authentication/session/CSRF semantics

docs/api/openapi.yaml
    -> exact paths, parameters, request/response schemas,
       enum values, nullable/required fields and Problem Details contract
```

The contract intentionally exposes public questionnaire rendering data but does not expose Assessment scoring metadata such as `dimension`, `keyedPole`, ambiguity/scoring rules or DefinitionVersion internals.

OpenAPI version `3.1.2` is used for the project contract. Although OpenAPI 3.2 exists, the current SpringDoc/Swagger-Core ecosystem used by Spring applications has not fully completed 3.2 support; 3.1.x is therefore the pragmatic implementation target for this MVP.

## Frontend executable-contract rule

The OpenAPI document is the accepted **design-first MVP target**, not a claim that every path is executable in the current Spring runtime. Group and historical deletion remain design-frozen. Step 8G-A implements provider-backed clarification routes but provider capability remains disabled until configuration and rollout.

The React frontend therefore uses implemented **Controller + DTO + SecurityConfig + accepted backend checkpoint behavior** as the executable baseline. This rule has now been exercised through F7-D: the frontend calls only implemented Identity/Assessment endpoints and deliberately does not instantiate runtime dependencies on Group, historical deletion or provider-backed clarification paths merely because they exist in the design-first target. Step 8G-B will add the React clarification interactions. Full-client generation can be reconsidered once the target contract and executable surface converge.

## Implementation checkpoints

### Step 8G-A — Backend HTTP adapter

- `POST start`, `POST messages` and `POST retry` use the owner inferred from the authenticated principal, require CSRF and delegate to the existing Application orchestrator.
- `GET current-turn` is a read-only, owner-scoped recovery query; it does not require a configured AI provider, does not re-invoke the model, and returns only the active unexpired question plus optimistic assistantSequence.
- Client payload validation uses Unicode code points and odd positive assistantSequence values; stale answers produce 409, invalid clarification lifecycle produces 422, unavailable AI capability produces 503.
- Internal execution tokens, provider prompts and temporary transcripts are never serialized in public DTOs.
- This implements the frozen v0.6.0 schema without changing it. It is not a Bedrock production activation; Frontend UX and AWS rollout remain separate.

## Contract version history

### v0.6.0 — 2026-10-12

Aligns the design-first provider-backed clarification contract with the hardened Step 8F-R2 runtime:

- adds `GET .../clarifications/{dimensionCode}/current-turn` for browser-refresh / lost-response recovery;
- exposes only the current temporary question plus `assistantSequence`, never the transcript or execution token;
- changes Continue to `assistantSequence + answer`;
- bounds user answers to 1000 Unicode code points and assistant questions to 500;
- defines `409 CLARIFICATION_TURN_CONFLICT` for temporary/current-turn conflicts;
- defines `503 AI_CLARIFICATION_UNAVAILABLE` for unavailable provider capability;
- preserves 422 for business-semantic clarification ineligibility;
- established these routes as a Step 8G contract; Step 8G-A now implements them behind the existing optional AI capability.

### v0.5.0 — 2026-10-05

Records the DefinitionVersion 1.1 contextual tie-break contract. Backend compatibility and Frontend dual-version support were deployed before activation; Flyway V8 has since promoted 1.1 to `AVAILABLE` for new Sessions while retained 1.0 Sessions remain supported:

- preserves immutable 1.0 direct-pole semantics and `USER_TIE_BREAK`;
- adds the version-aware tie-break interaction design for 1.1;
- adds the contextual `questionId + selectedOptionId` request shape and `TIE_BREAK_QUESTION`;
- keeps contextual option-to-pole mappings Backend-owned;
- keeps HTTP paths under `/api/v1`;
- requires existing 1.0 Sessions to remain completable after 1.0 is retired for new starts.

The executable Backend understands both tie-break request/interaction variants by Session-bound DefinitionVersion. V8 activates 1.1 for new Sessions, while Sessions already bound to retired 1.0 continue using the legacy request/interaction shape.

### v0.4.0 — 2026-09-30

Aligns the design-first contract with accepted Step 7 deterministic Clarification/Tie-break HTTP behavior:

- `skip` and `skip-remaining` return the authoritative `AssessmentSessionResponse`.
- `skip-remaining` explicitly documents the implemented `422 Unprocessable Content` business-semantic response.
- the tie-break route remains a `PUT` of the selected pole only and returns the updated Session.
- Start / Continue / Retry clarification interaction routes remain design-frozen for Step 8 and are not evidence that real provider-backed runtime interaction is already implemented.

No Group endpoint is implemented by this version bump; those paths remain part of the accepted design baseline.

### v0.3.0 — 2026-09-28

Synchronizes the design-first contract with accepted Assessment implementation through Step 6:

- Step 5 Restart / Start New contract is present, including `201 Created`, `Location`, and `RestartAssessmentResponse`.
- `AssessmentHistoryItem` now matches the implemented Step 6 response: `sessionId`, `assessmentCode`, `assessmentVersion`, `finalType`, `completedAt`.
- obsolete `assessmentName`, `id`, and `version` History fields from v0.2.0 are removed/renamed.

The version was bumped deliberately rather than silently editing the previously accepted DTO contract.

## Ownership rule

The OpenAPI document is design-first, but once implementation feedback is explicitly accepted the contract must be updated deliberately to match that decision. Spring implementation and generated documentation must not drift silently from the accepted contract.
