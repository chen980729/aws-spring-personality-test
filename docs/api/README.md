# API Contract

> **Status:** OpenAPI v0.4.0 — active accepted design contract
> **Last updated:** 2026-10-03
> **Implementation alignment:** Identity/Security + deterministic Assessment HTTP behavior through Step 7; consumed by the React frontend through F6

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

The OpenAPI document is the accepted **design-first MVP target**, not a claim that every path is executable in the current Spring runtime. In particular, Group, historical deletion and provider-backed clarification interaction can remain design-frozen before their Controllers are implemented.

The React frontend therefore uses implemented **Controller + DTO + SecurityConfig + accepted backend checkpoint behavior** as the executable baseline. This rule has now been exercised through F6: the frontend calls only implemented Identity/Assessment endpoints and deliberately does not instantiate runtime dependencies on Group, historical deletion or provider-backed clarification paths merely because they exist in the design-first target. Full-client generation can be reconsidered once the target contract and executable surface converge.

## Contract version history

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
