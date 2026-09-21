# API Contract

> **Status:** OpenAPI v0.2.0 — Accepted / Ready for Implementation  
> **Last updated:** 2026-09-21

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

OpenAPI version `3.1.2` is used for the project contract. Although OpenAPI 3.2 exists, the current SpringDoc/Swagger-Core generation ecosystem used by Spring applications has not fully completed 3.2 support; 3.1.x is therefore the pragmatic implementation target for this MVP.

The OpenAPI document is design-first. Spring implementation and any generated documentation must conform to it rather than silently changing it.
