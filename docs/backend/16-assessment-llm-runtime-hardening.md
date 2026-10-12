# Assessment LLM Runtime Hardening Checkpoint

> **Checkpoint:** Step 8F-R  
> **Date:** 2026-10-12  
> **Status:** Runtime hardening complete; Step 8F-R2 contract consistency guards follow before public HTTP/Frontend exposure

## 1. Scope

Steps 8B through 8F established the internal AI clarification runtime:

~~~
provider-neutral ClarificationAiPort
temporary PostgreSQL runtime context
Amazon Bedrock Converse transport
prompt + tool contract + strict backend validation
two-transaction runtime orchestration
~~~

Step 8F-R hardens that runtime before Step 8G exposes Start / Continue / Retry publicly.

No live Bedrock production call or final model selection is accepted by this checkpoint.

## 2. Temporary conversation lifecycle

Clarification messages are non-authoritative runtime state.

Default policy:

~~~
context TTL = 1 hour
scheduled cleanup interval = 15 minutes
in-flight external-call lease = 60 seconds
~~~

The TTL is sliding across one execution: appending a new message refreshes the common expiry for the whole active transcript.

Immediate cleanup occurs after:

~~~
accepted judgement
accepted FAILED_RETRYABLE transition
Skip one
Skip remaining
Restart / abandon Session
~~~

Scheduled cleanup calls the idempotent:

~~~
deleteExpired(now)
~~~

Multiple ECS tasks may execute the cleanup safely.

## 3. Crash and rolling-deployment recovery

The normal runtime boundary remains:

~~~
Transaction A
  persist local state
  commit

external AI call
  no database transaction

Transaction B
  re-lock
  revalidate execution token + sequence
  commit result/failure
~~~

A task can disappear after Transaction A because of rolling deployment, process termination or infrastructure failure.

A short execution lease therefore distinguishes:

~~~
context still available for user interaction
~~~

from:

~~~
an external AI call that should no longer be assumed alive
~~~

Retry can normalize an expired IN_PROGRESS execution to FAILED_RETRYABLE before creating a new execution token.

Covered orphan shapes include:

~~~
IN_PROGRESS + empty runtime context
IN_PROGRESS + latest USER_ANSWER
expired active context
~~~

An active assistant question that has not expired is not considered an orphan; it is waiting for the user.

## 4. Unexpected adapter failure

Expected provider/transport/schema failures use:

~~~
ClarificationAiTechnicalException
-> FAILED_RETRYABLE
~~~

Unexpected RuntimeException from the AI adapter is handled differently:

~~~
best-effort FAILED_RETRYABLE compensation
-> rethrow original exception
-> HTTP 500 / operational visibility later
~~~

This restores user workflow state without disguising programming defects as normal provider outages.

The compensation catch surrounds the external AI call only; it does not swallow arbitrary persistence/finalization bugs.

## 5. Input and output bounds

Runtime text limits are:

~~~
USER_ANSWER <= 1000 Unicode code points
ASSISTANT_QUESTION <= 500 Unicode code points
~~~

The user limit is checked using Unicode code points rather than Java UTF-16 code units, so Chinese, Japanese and supplementary-plane characters are treated consistently.

Flyway V10 adds matching PostgreSQL constraints.

The Bedrock follow-up tool schema also declares the assistant-question maximum, and the response parser rejects oversized provider output.

## 6. Prompt revision and provenance

ClarificationPolicy.revision is no longer only copied into provenance.

The Bedrock request factory resolves:

~~~
ClarificationPromptPolicyRegistry
        ↓
revision v1
        ↓
bounded system instructions
+ neutral target-dimension pole semantics
~~~

Unknown revisions are technical failures and never reach the provider.

Policy v1 defines neutral semantics for:

~~~
E / I
S / N
T / F
J / P
~~~

so the provider is not expected to infer the meaning of a single-letter pole from prior model knowledge.

A material prompt-policy change requires a new policy revision. This keeps accepted AIProvenance.clarificationPolicyRevision meaningful.

## 7. Output strictness

The backend intentionally retains strict semantic validation.

For example:

~~~
UNCLEAR + suggestedPole
~~~

is rejected as inconsistent provider output rather than silently repaired.

The rule remains:

~~~
technical/invalid provider output != business UNCLEAR
~~~

If model evaluation later shows frequent schema violations, improve prompt/schema/model selection rather than weakening authoritative backend validation.

## 8. Provider/runtime bean boundary

ClarificationRuntimeOrchestrator is no longer defined inside BedrockRuntimeConfiguration.

Infrastructure runtime configuration creates the orchestrator only when a ClarificationAiPort bean exists.

This keeps orchestration provider-neutral and allows a future provider adapter to replace Bedrock without moving Application workflow code.

Step 8F-R2 freezes unavailable AI capability as `503 AI_CLARIFICATION_UNAVAILABLE` for provider-backed commands rather than treating absence as UNCLEAR. It also separates temporary/current-turn conflicts as `409 CLARIFICATION_TURN_CONFLICT` from semantic eligibility failures that remain 422.

## 9. PostgreSQL and backup boundary

PostgreSQL remains the temporary context store for MVP because it already exists, survives ECS task replacement and supports the same transaction/concurrency boundaries.

The runtime table is not a historical transcript store.

Current Terraform configures RDS automated backup retention for one day. Live-row deletion therefore does not guarantee that text immediately disappears from already-created automated backups.

See ADR-0008 for the accepted retention/privacy decision.

## 10. Step 8G entry criteria

Before public AI endpoints and Frontend interaction are added:

~~~
Skip cleanup integration test passes
Restart cleanup integration test passes
expired in-flight execution can recover through Retry
unexpected adapter runtime failure restores retryable workflow state
V10 length constraints pass
prompt revision mismatch fails before provider invocation
full backend regression passes
~~~

After this checkpoint, Step 8G may expose:

~~~
POST .../clarifications/{dimensionCode}/start
POST .../clarifications/{dimensionCode}/messages
POST .../clarifications/{dimensionCode}/retry
~~~

The Continue request will include both:

~~~
assistantSequence
answer
~~~

The internal execution token remains server-only.
