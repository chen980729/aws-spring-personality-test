# ADR-0008: Keep AI Clarification Transcript Non-authoritative and Short-lived

- Status: Accepted, refined 2026-10-12
- Original decision date: 2026-09-15

## Context

AI clarification may involve multiple conversational turns and may contain personal free-text descriptions.

The original MVP decision correctly rejected the clarification transcript as authoritative Assessment history, but its wording also said that temporary conversation context would not be persisted. Step 8 implementation feedback changed that persistence detail.

The deployed application runs multiple/replaced ECS tasks. Keeping active clarification context only in process memory would make an ordinary task replacement or rolling deployment lose the current turn immediately. Adding Redis/ElastiCache only for a very small MVP transcript would add another managed service, security surface, cost and operational concern.

PostgreSQL is already required for the Assessment workflow and supports the execution-token and sequence guards needed by clarification concurrency.

## Decision

The full AI clarification transcript remains **non-authoritative runtime state**. It is not part of durable Assessment history and is never used as a long-term audit log.

For an active clarification execution only, the Backend may temporarily persist normalized messages in PostgreSQL:

~~~
ASSISTANT_QUESTION
USER_ANSWER
ASSISTANT_QUESTION
USER_ANSWER
~~~

The runtime row contains only:

~~~
clarification reference
execution token
sequence number
message role
message text
created timestamp
expiry timestamp
~~~

It does not persist raw Bedrock request/response objects, tool-use IDs, provider JSON, user identity fields, email, IP address, or the system prompt.

After an accepted clarification result, authoritative history still consists only of:

~~~
ClarificationResult
AIProvenance
accepted timestamps
~~~

### Retention

The default active-context TTL is **1 hour**.

The expiry is sliding: when a new runtime message is appended, all messages for that execution receive the same refreshed expiry.

Physical deletion is enforced in two ways:

1. lifecycle cleanup in the same database transaction when the context is no longer useful;
2. scheduled expired-row cleanup as a safety net.

Lifecycle cleanup occurs on:

~~~
accepted judgement
accepted FAILED_RETRYABLE transition
Skip one clarification
Skip remaining clarifications
Restart / abandon the AssessmentSession
~~~

A scheduled cleanup job runs every 15 minutes by default and deletes rows whose expires_at is in the past. The delete is idempotent, so more than one ECS task may run the job safely.

### In-flight execution recovery

Context TTL and AI-call liveness are separate concepts.

The default in-flight execution lease is **60 seconds**. It bounds how long an external AI call may be assumed to still be running after local state was committed.

Retry may recover an IN_PROGRESS clarification when:

~~~
the active execution has no runtime message
and its execution lease expired

OR

the latest runtime message is USER_ANSWER
and its execution lease expired

OR

the temporary context itself expired
~~~

Recovery first normalizes the old execution to FAILED_RETRYABLE, deletes its temporary context, and then creates a fresh execution token through the normal Retry lifecycle.

Unexpected RuntimeException from the AI adapter is also compensated to FAILED_RETRYABLE when possible and then rethrown so programming defects are not hidden.

### Input bounds

To limit cost, storage abuse and prompt-injection surface:

~~~
USER_ANSWER <= 1000 Unicode code points
ASSISTANT_QUESTION <= 500 Unicode code points
~~~

Application validation enforces the limits and Flyway V10 adds a PostgreSQL CHECK constraint as a persistence backstop.

### Backup boundary

Application-level deletion does not imply immediate erasure from every database backup.

The current Terraform configuration uses:

~~~
RDS backup_retention_period = 1 day
~~~

A deleted temporary message may therefore remain inside an automated RDS backup until that backup leaves the configured retention window.

This backup-retention boundary must be considered separately from the live-table 1-hour runtime TTL.

## Why PostgreSQL instead of alternatives

### PostgreSQL — selected

Benefits:

- already required by the application;
- survives ECS task replacement;
- supports the same transactions used for lifecycle cleanup;
- supports execution-token and sequence concurrency guards;
- avoids adding another AWS managed service for a very small MVP workload.

Cost:

- personal free text temporarily enters durable storage;
- backup retention outlives live-row deletion;
- scheduled cleanup is required.

### In-memory context — rejected

It would be simple but ordinary ECS replacement or rolling deployment would lose active clarification state immediately.

### Redis / ElastiCache — deferred

It is a reasonable future option for high-volume short-lived state, but it adds infrastructure, networking, security and cost that are not justified by the current portfolio MVP.

### Browser-only transcript — rejected

The Backend must validate turn ordering and reconstruct trusted provider requests; relying on a client-supplied transcript would weaken the server-side authority boundary.

## Prompt and provenance relationship

AIProvenance.clarificationPolicyRevision identifies the accepted clarification policy semantics.

The Backend therefore resolves the system prompt through a revision-aware ClarificationPromptPolicyRegistry.

The current implementation supports:

~~~
clarificationPolicyRevision = v1
~~~

Unknown revisions fail before provider invocation.

Policy v1 contains the bounded system instructions and neutral EI/SN/TF/JP pole semantics. This avoids relying on the model's prior knowledge of single-letter pole codes.

A material prompt-policy change must introduce a new revision rather than silently changing the semantics attributed to v1.

## Consequences

### Benefits

- Transcript is not promoted into authoritative Assessment history.
- Active interaction survives normal ECS replacement better than process-local state.
- Privacy exposure is bounded by lifecycle deletion plus short TTL cleanup.
- Execution lease prevents a dead external call from trapping a clarification indefinitely.
- Prompt provenance becomes meaningfully tied to versioned semantics.
- No Redis/ElastiCache dependency is required for MVP.

### Costs

- Temporary personal text exists in PostgreSQL.
- Automated backups may retain deleted text for their separate retention period.
- Exact mid-turn resume is not guaranteed after an orphaned AI call; recovery starts a fresh execution.
- A scheduler and execution-recovery policy are now operational responsibilities.

## Future work

If future product requirements need complete AI audit history, introduce a separate explicit audit model such as ClarificationAttempt[] with its own retention/privacy policy. Do not repurpose the temporary runtime-message table as historical evidence.
