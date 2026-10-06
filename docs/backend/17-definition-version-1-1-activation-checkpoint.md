# DefinitionVersion 1.1 Activation Checkpoint

> **Status:** Activation release deployed and production-verified
> **Date:** 2026-10-06
> **Scope:** DefinitionVersion availability switch only; no questionnaire, scoring, API, or Frontend contract changes

## 1. Purpose

The compatibility release introduced DefinitionVersion 1.1 as a complete but non-executable `DRAFT` while keeping 1.0 `AVAILABLE`. That allowed the Backend and Frontend to learn both tie-break semantics before any normal user Session could bind to 1.1.

This checkpoint records the separate promotion step that completes the expand-then-promote rollout.

## 2. Flyway activation

`V8__activate_sixteen_personality_v1_1.sql` performs the production state transition:

```text
before V8
1.0  AVAILABLE
1.1  DRAFT

after V8
1.0  RETIRED
1.1  AVAILABLE
```

The migration retires 1.0 before promoting 1.1 so the partial unique index that permits at most one `AVAILABLE` version is never violated.

The migration also verifies that exactly one expected 1.0 row was retired and exactly one expected 1.1 row was promoted. An unexpected catalog state therefore fails the migration instead of silently leaving a partial activation.

1.1 receives a fixed `published_at` timestamp when it becomes `AVAILABLE`. V1-V7 remain unchanged.

## 3. Session binding semantics

Activation changes only which version may be bound by a **new** Session.

- A normal Start Assessment with no active Session resolves the sole `AVAILABLE` version and therefore binds 1.1 after V8.
- An existing 1.0 Session keeps its original `definition_version_id`.
- Resuming a retained 1.0 Session continues to use FinalizationPolicy v1 and direct-pole tie-break semantics.
- A new 1.1 Session uses FinalizationPolicy v2 and contextual tie-break questions.
- Historical results are not reinterpreted and no question/option provenance is invented for 1.0 facts.

`RETIRED` means “not eligible for new Session binding”; it does not mean “unreadable” or “unexecutable for an already-bound Session”.

## 4. Activation-specific regression coverage

The activation release verifies:

1. the repository resolves DefinitionVersion 1.1 as the sole `AVAILABLE` version;
2. DefinitionVersion 1.0 remains loadable as `RETIRED`;
3. normal `StartAssessmentService` creates a new Session bound to 1.1;
4. contextual 1.1 tie-break completion still records `TIE_BREAK_QUESTION`;
5. a Session explicitly bound to retired 1.0 still exposes the legacy direct-pole interaction;
6. the immutable 1.0/1.1 specification-equivalence checks still compare the two exact versions rather than relying on whichever version is currently available.

These tests are intentionally different from the pre-activation acceptance in checkpoint 16: the database fixture now represents the post-promotion production catalog.

## 5. Deployment verification

Production verification is complete:

1. the activation commit passed the `main` CI workflow;
2. successful CI triggered the CD workflow automatically through `workflow_run`;
3. Backend ECS deployment and Frontend S3/CloudFront deployment both succeeded;
4. the post-deployment public smoke job passed;
5. a new production Assessment was confirmed to use DefinitionVersion 1.1 behavior;
6. an exact-tie production flow was confirmed to persist/report `TIE_BREAK_QUESTION`, proving the contextual tie-break path is active.

Retained 1.0 backward compatibility remains covered by the regression suite: an existing Session bound to 1.0 keeps the legacy direct-pole semantics even though 1.0 is no longer eligible for new binding.

The activation migration itself is the release boundary. No manual production SQL update is performed outside Flyway.

## 6. Architectural result

The complete rollout is now:

```text
V6 persistence expansion
        ↓
V7 seed 1.1 DRAFT
        ↓
dual-version Backend + Frontend deployment
        ↓
old ECS tasks drain
        ↓
V8 retire 1.0 / promote 1.1
```

This demonstrates why DefinitionVersion is an immutable runtime contract rather than a mutable configuration row: compatibility can be deployed first, activation can happen later, and old Sessions remain explainable and executable.
