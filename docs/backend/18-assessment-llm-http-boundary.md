# Assessment LLM HTTP Boundary — Step 8G-A

> **Date:** 2026-10-14
> **Scope:** Backend HTTP adapter for frozen OpenAPI v0.6.0
> **Status:** Implementation PR; React and provider rollout remain separate.

## 1. Responsibility boundary

```text
Browser / future React 8G-B
    |
    +-- POST start / messages / retry -- Session authentication + CSRF
    |       |
    |       v
    |   ClarificationRuntimeController
    |       |
    |       v
    |   ClarificationRuntimeOrchestrator
    |       |--- short prepare transaction (locks + state check)
    |       |--- optional ClarificationAiPort (outside DB transaction)
    |       `--- short commit transaction (token + sequence revalidation)
    |
    `-- GET current-turn ------------ Session authentication, no CSRF
            |
            v
        GetCurrentClarificationTurnService
            |--- verify owned Session before reading runtime data
            |--- check bound DefinitionVersion/Dimension
            |--- require unexpired latest ASSISTANT_QUESTION
            `--- return assistantSequence + question only
```

The read path intentionally does **not** invoke the AI provider, mutate the Session,
renew TTL, expose historical messages, or expose the internal execution token.

## 2. Frozen endpoints

| Method | Path suffix | Body / response | Expected behavior |
|---|---|---|---|
| POST | `/{sessionId}/clarifications/{dimensionCode}/start` | No body / InteractionResponse | Begin eligible clarification and invoke AI provider |
| GET | `/{sessionId}/clarifications/{dimensionCode}/current-turn` | No body / CurrentTurnResponse | Read current answerable question for refresh/recovery |
| POST | `/{sessionId}/clarifications/{dimensionCode}/messages` | `{assistantSequence, answer}` / InteractionResponse | Answer only the optimistic current turn |
| POST | `/{sessionId}/clarifications/{dimensionCode}/retry` | No body / InteractionResponse | Retry only eligible FAILED_RETRYABLE or recoverable executions |

All paths are beneath `/api/v1/assessment-sessions`. The authenticated user,
not a client-controlled userId, is passed into Application commands.

The public `ClarificationInteractionResponse` carries the authoritative Session
status, clarification status, workflow and optional accepted result. For a follow-up
it includes only the new question and the odd assistantSequence; after a
terminal result, both fields are null.

## 3. Failure and recovery decisions

| Case | HTTP | Stable Problem Details code |
|---|---|---|
| Missing/foreign Session | 404 | `ASSESSMENT_SESSION_NOT_FOUND` |
| Stale assistantSequence, still-starting call, still-processing answer | 409 | `CLARIFICATION_TURN_CONFLICT` |
| Current question missing, expired or temporarily not answerable | 409 | `CLARIFICATION_TURN_CONFLICT` |
| Invalid Session/Dimension clarification lifecycle | 422 | `CLARIFICATION_NOT_ALLOWED` |
| AI provider capability disabled | 503 | `AI_CLARIFICATION_UNAVAILABLE` |
| Invalid input shape / answer constraints | 400 | `VALIDATION_FAILED` / `MALFORMED_JSON` |

The client must not automatically replay a failed POST after a 409.
Instead, it should GET the authoritative Session and current-turn to decide
whether to render the new question, present Retry, or offer Skip.

The unavailable-provider 503 applies to provider-backed POST routes only.
The recovery GET remains usable without any `ClarificationAiPort` bean.

## 4. Security and privacy

- Spring Session authentication applies to all four routes.
- CSRF applies to POST routes. Recovery GET is safe/read-only.
- Session ownership is checked before reading target Dimension runtime data.
- DimensionCode must exist in the Session's immutable bound DefinitionVersion.
- No API response contains execution tokens, transcript history, questionnaire
  scoring keys, provider payloads or system instructions.
- The HTTP answer is trimmed and limited to 1000 Unicode code points;
  `assistantSequence` must be a positive odd integer.
- The provider remains disabled by default; adding HTTP surface does not grant
  ECS Bedrock permissions or enable live inference.

## 5. Verification and remaining work

Step 8G-A tests cover WebMvc Security/CSRF, optimistic turn binding, DTO privacy,
stable Problem Details, disabled-provider 503, and PostgreSQL-backed recovery
of the latest unexpired assistant question without a model call.

Step 8G-B will implement the React flow, state refresh, in-flight UI,
409 recovery and Retry/Skip choices. Step 8H configures AWS IAM, Terraform and ECS
environment. Step 8I tests live Bedrock models and safety/cost/latency.

See `docs/backend/17-assessment-llm-contract-consistency.md` for the
pre-8G invariants and `docs/api/openapi.yaml` for exact schemas.
