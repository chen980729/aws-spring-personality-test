# Step 8G-B — React AI Clarification Interaction

> **Date:** 2026-10-18
> **Status:** Implementation PR; pending merge and subsequent AWS Bedrock activation
> **Depends on:** PR #21 / Step 8G-A Backend HTTP boundary and OpenAPI v0.6.0

## 1. Purpose and boundaries

Step 8G-B connects the existing React Assessment Session page to the public AI
Clarification endpoints without duplicating Backend lifecycle logic.

It deliberately does **not** enable Amazon Bedrock in production. The default
`BEDROCK_CLARIFICATION_ENABLED=false` means POST Start/Continue/Retry returns
503 `AI_CLARIFICATION_UNAVAILABLE`; the UI explains the limitation and keeps
Skip / Skip Remaining functional. AWS IAM, ECS wiring and live-model evaluation
are separate Steps 8H and 8I.

## 2. Presentation and authoritative state

```text
AssessmentSessionPage
   |
   +--> useAssessmentSessionQuery(sessionId)
   |        |
   |        v
   |     Backend-owned AssessmentWorkflow
   |        |
   |        v
   |     assessmentWorkflowResolver
   |        |
   |        v
   |     ClarificationStage
   |        +-- mode=PENDING          -> explicit POST /start
   |        +-- mode=IN_PROGRESS      -> GET /current-turn
   |        |                           render current question + answer
   |        |                           POST /messages (assistantSequence, answer)
   |        +-- mode=FAILED_RETRYABLE -> explicit POST /retry
   |        `-- all modes            -> explicit Skip/Skip Remaining fallback
   |
   `--> TanStack Query cache:
        session(sessionId)               authoritative workflow
        currentClarificationTurn(id,dim) answerable question/version only
```

The UI never derives a new Clarification status by itself. Each successful
AI POST provides only a partial public interaction response; the client updates
the current-question cache and re-queries the complete Session to drive
navigation to the next Clarification, Tie-break, or Result stage.

Free-form answer drafts remain local React state and are tied to the currently
displayed odd `assistantSequence`. A newly observed question resets the
effective draft, so an earlier text cannot be silently applied to a new turn.

## 3. Recovery and error handling

The browser may reload after a POST succeeds but before the HTTP response
reaches it. This is intentionally handled by reading, not retrying:

```text
Refresh page / lost response / 409 stale turn
    |
    +--> GET authoritative Session
    `--> GET current-turn (only if Session shows IN_PROGRESS)
         +-- 200: show latest assistantSequence + question
         +-- 409: no answerable question; explain still processing / expiry
         `-- state changed: Session workflow determines next UI
```

A `409 CLARIFICATION_TURN_CONFLICT` or ambiguous network failure **never**
automatically resubmits the previous answer. The user sees a recovery prompt
and can manually refresh status. A `503 AI_CLARIFICATION_UNAVAILABLE` does
not trigger a model retry and leaves deterministic Skip options usable.

The shared HttpClient owns Session cookies and CSRF handling; the
Clarification UI never constructs CSRF headers or accepts a client user ID.

## 4. Privacy and bounds

Only the current assistant question and optimistic turn version reach the UI.
Internal execution tokens, temporary transcript, prompt policy, scoring keys,
and provider payloads remain server-side.

The browser trims answers and measures the limit using Unicode code points
(`Array.from(text.trim()).length`), matching the Backend's 1000-code-point
validation. Button actions are disabled while a model-backed mutation is
in flight; no automatic provider command retries occur.

## 5. Tests and acceptance

React Testing Library + MSW checks:
- provider-disabled Start and Retry => 503 and explicit Skip fallback;
- refresh of an active Session => GET current-turn, no duplicate Start;
- Continue sends the exact question `assistantSequence` and trimmed answer;
- 409 stale Continue refreshes current-turn without replaying POST;
- missing/processing current turn remains a recoverable state;
- existing deterministic skip/tie-break, frontend lint/build, and Backend CI
  remain unchanged.

After this PR merges, no production live AI calls should be claimed until
AWS configuration and an explicit Bedrock smoke test succeed.
