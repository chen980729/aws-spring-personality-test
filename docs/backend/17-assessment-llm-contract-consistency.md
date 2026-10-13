# Assessment LLM Contract Consistency Checkpoint

> **Checkpoint:** Step 8F-R2  
> **Date:** 2026-10-12  
> **Status:** Accepted pre-8G contract consistency guard

## 1. Purpose

Step 8F-R hardened runtime lifecycle and privacy. Step 8F-R2 closes the remaining consistency gaps before public HTTP/Frontend implementation.

The checkpoint does not expose provider-backed Controllers yet. It freezes the invariants that Step 8G must implement.

## 2. DefinitionVersion and prompt-policy compatibility

Every persisted AssessmentDefinitionVersion must reference a clarification policy revision that exists in ClarificationPromptPolicyRegistry.

The integration test loads every seeded DefinitionVersion after Flyway and verifies:

~~~text
clarificationPolicy.revision
    ∈ registry.supportedRevisions()

each DimensionCode
    exists in that policy

each DimensionDefinition poleA / poleB
    exists in that dimension's prompt semantics
~~~

This turns a future migration such as a new v2 policy into a CI/deployment-time requirement rather than a user-visible infinite FAILED_RETRYABLE loop.

A missing prompt revision or dimension/pole mapping is a configuration/programming defect, not a transient provider outage.

## 3. Prompt semantic fingerprint

Policy v1 is protected by a SHA-256 semantic fingerprint over:

~~~text
revision
base instructions
final-turn instructions
sorted dimension labels
pole codes
pole labels
neutral pole descriptions
~~~

Java source formatting is not part of the fingerprint.

If a material v1 prompt/policy semantic changes, the test fails with the intended resolution:

~~~text
revert the semantic change
OR
introduce a new clarification policy revision
~~~

Do not silently mutate the meaning attributed to accepted AIProvenance revision v1.

## 4. Current-turn recovery contract

A persisted unexpired ASSISTANT_QUESTION is valid user-facing runtime state, not an orphan.

Step 8G must expose:

~~~text
GET /api/v1/assessment-sessions/{sessionId}
    /clarifications/{dimensionCode}/current-turn
~~~

Success returns only:

~~~json
{
  "sessionId": "uuid",
  "dimensionCode": "EI",
  "assistantSequence": 1,
  "question": "..."
}
~~~

It does not return prior conversation messages, execution token, provider payload or prompt internals.

This supports:

~~~text
browser refresh
lost Start response
lost Continue response
~~~

without re-invoking the model.

Retry must not discard a valid unexpired question merely because the client lost its previous HTTP response.

## 5. HTTP error taxonomy

Step 8G must use stable codes rather than branching on exception text.

~~~text
409 CLARIFICATION_TURN_CONFLICT

- execution still starting
- previous answer still processing
- stale assistantSequence
- Retry while a valid current question is waiting
~~~

~~~text
422 CLARIFICATION_NOT_ALLOWED

- clarification action is semantically invalid for the current
  Session/Dimension lifecycle
~~~

~~~text
503 AI_CLARIFICATION_UNAVAILABLE

- provider capability required by Start / Continue / Retry is unavailable
~~~

~~~text
500

- unexpected programming/configuration defect
~~~

Frontend recovery from 409 is to re-read Session/current-turn state.

## 6. Bedrock output budget

Default provider output budget is:

~~~text
BEDROCK_CLARIFICATION_MAX_TOKENS = 1024
~~~

The increase from 512 provides tokenizer/tool-call headroom across candidate models and CJK output.

It does not weaken the accepted business output bounds:

~~~text
ASSISTANT_QUESTION <= 500 Unicode code points
USER_ANSWER <= 1000 Unicode code points
maximum model calls = 3
~~~

Actual token usage, stop reasons, latency and cost must still be evaluated during the model bake-off.

## 7. Provider timeout and execution lease invariant

The provider-neutral runtime currently uses:

~~~text
in-flight execution lease = 60s
~~~

Bedrock currently uses:

~~~text
apiCallTimeout = 20s
~~~

Infrastructure validates at application startup:

~~~text
inFlightExecutionLease
    > apiCallTimeout + 5s safety margin
~~~

This validation belongs in Bedrock Infrastructure, not the provider-neutral Application policy.

A future provider adapter must establish its own equivalent compatibility validation.

## 8. Step 8G entry condition

Step 8G may now implement the frozen public interaction surface:

~~~text
POST .../clarifications/{dimensionCode}/start
GET  .../clarifications/{dimensionCode}/current-turn
POST .../clarifications/{dimensionCode}/messages
POST .../clarifications/{dimensionCode}/retry
~~~

with:

~~~text
assistantSequence + answer
409 / 422 / 503 taxonomy
current-turn refresh recovery
server-only execution token
~~~
