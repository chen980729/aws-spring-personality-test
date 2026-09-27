# API Contract

> **Status:** OpenAPI v0.3.0 — active accepted contract
> **Last updated:** 2026-09-28
> **Implementation alignment:** Identity/Security + Assessment through Step 6

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

## Contract version history

### v0.3.0 — 2026-09-28

Synchronizes the design-first contract with accepted Assessment implementation through Step 6:

- Step 5 Restart / Start New contract is present, including `201 Created`, `Location`, and `RestartAssessmentResponse`.
- `AssessmentHistoryItem` now matches the implemented Step 6 response: `sessionId`, `assessmentCode`, `assessmentVersion`, `finalType`, `completedAt`.
- obsolete `assessmentName`, `id`, and `version` History fields from v0.2.0 are removed/renamed.

The version was bumped deliberately rather than silently editing the previously accepted DTO contract.

## Ownership rule

The OpenAPI document is design-first, but once implementation feedback is explicitly accepted the contract must be updated deliberately to match that decision. Spring implementation and generated documentation must not drift silently from the accepted contract.
