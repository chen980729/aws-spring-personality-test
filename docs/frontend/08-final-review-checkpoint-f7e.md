# Frontend F7-E — Final Review / Release Readiness Checkpoint

> **Post-release status (2026-10-06):** The compatibility release and separate DefinitionVersion 1.1 activation release are now deployed. Automatic post-CI CD has been verified end-to-end. The release-readiness decisions below are retained as the historical F7-E checkpoint; README screenshot refresh remains a separate visual-maintenance task.

> **Status:** Review complete — compatibility release ready
> **Date:** 2026-10-06
> **DefinitionVersion state:** 1.0 = AVAILABLE, 1.1 = DRAFT
> **Release decision:** deploy compatibility release first; activation remains a separate release

## 1. Scope

F7-E is not a new product feature.

It reviews the complete DefinitionVersion 1.1 compatibility/presentation batch:

```text
Backend Step 2A–2E
        +
Frontend F7-A
  dual-version API/types
        +
Frontend F7-B
  contextual Tie-break UI
        +
Frontend F7-C
  Result content + per-type images
        +
Frontend F7-D
  Landing hero
        ↓
F7-E
  final regression
  documentation alignment
  rollout/release review
```

## 2. Final architecture review

The accepted ownership boundaries still hold.

### Backend remains authoritative for assessment semantics

Backend owns:

- Session-bound DefinitionVersion;
- scoring and ambiguity;
- Tie-break question definitions;
- contextual option-to-pole mappings;
- accepted Tie-break provenance;
- final preference and final type;
- final decision source.

Frontend does not receive or infer contextual option-to-pole mappings.

### Frontend owns presentation and user intent

Frontend owns:

- version-aware rendering;
- question/option presentation;
- user-selection intent;
- result interpretation copy;
- static illustrations;
- responsive visual composition.

Personality Content Version 1.0 remains independent from AssessmentDefinitionVersion.

Changing Result copy or images does not change assessment semantics.

## 3. Dual-version compatibility review

Retained 1.0 behavior:

```text
1.0 Session
  -> existing direct-pole Tie-break UI
  -> no dependency on new Tie-break GET endpoint
  -> PUT { selectedPole }
  -> USER_TIE_BREAK provenance
```

Staged 1.1 behavior:

```text
1.1 Session
  -> GET contextual interaction
  -> instruction + prompt + option text only
  -> PUT { questionId, selectedOptionId }
  -> Backend resolves pole
  -> TIE_BREAK_QUESTION provenance
```

This asymmetry is intentionally safe for the current parallel Backend/Frontend CD workflow.

During the compatibility release:

- new Frontend + old Backend still supports 1.0;
- new Backend + old Frontend cannot create a 1.1 Session because 1.1 remains DRAFT;
- V6 expands persistence without changing legacy facts;
- V7 seeds 1.1 as DRAFT without changing availability.

## 4. Result presentation resilience review

F7-E identified one presentation-boundary issue.

Before review:

```text
Backend returns valid final result
        ↓
Frontend Content Version has no matching type copy
        ↓
Result page returned presentation error
        ↓
authoritative final type + Decision trace hidden
```

That contradicted the project rule that frontend presentation content is not business truth.

F7-E changes the fallback to:

```text
Backend returns valid final result
        ↓
presentation copy unavailable
        ↓
show authoritative final type
show explanatory-content notice
keep Decision trace visible
omit unsupported interpretation/image only
```

The normal sixteen supported types are unchanged.

Result illustrations now also provide their intrinsic 320 × 320 dimensions.

## 5. Test / CI review

Final review head:

```text
Frontend:
  30 test files
  103 tests
  lint: success
  production build: success

Backend:
  success

Backend Docker Image:
  success
```

Coverage represented in the batch includes:

- retained 1.0 direct-pole workflow;
- 1.1 contextual GET/PUT contract;
- mixed request rejection through typed/backend boundaries;
- contextual CSRF path;
- same-value retry / conflict behavior;
- contextual final source presentation;
- Result content catalog completeness;
- 16 type-image mappings;
- Result interpretation + Decision trace;
- missing presentation-copy graceful degradation;
- Landing CTA and hero asset contract;
- Landing accessibility regression;
- Backend PostgreSQL contextual persistence/concurrency acceptance.

Dedicated Playwright E2E remains deferred and is not required to re-prove the already-covered domain/API branches.

## 6. Documentation review

Living documents were aligned through F7-E.

Updated current-state materials include:

- root English README;
- root Japanese README;
- docs index;
- requirements;
- architecture;
- Assessment Domain;
- roadmap;
- OpenAPI README;
- ADR-0018;
- frontend architecture/API/testing/checkpoints.

Historical checkpoints retain their original temporal meaning. Where a historical checkpoint had a now-completed item, a post-checkpoint progress note is used instead of rewriting history.

## 7. Release sequencing decision

**DefinitionVersion 1.1 must not be activated in this compatibility PR.**

Current Flyway state intentionally ends at:

```text
V6 -> persistence expansion
V7 -> seed 1.1 DRAFT
```

There is no activation migration in this release.

Safe sequence:

```text
1. squash-merge compatibility PR
2. main CI succeeds
3. automatic CD deploys Backend + Frontend
4. public smoke test succeeds
5. verify ECS service is fully stable / old tasks drained
6. verify fresh frontend bundle is served
7. only then create a separate activation PR/migration
8. retire 1.0 for new starts
9. promote 1.1 to AVAILABLE
10. verify retained 1.0 Session + new 1.1 Session
```

The database availability transition must retire 1.0 before promoting 1.1 because the schema enforces at most one AVAILABLE version.

Existing 1.0 Sessions remain bound to 1.0 and must remain completable after 1.0 becomes RETIRED.

## 8. Stale browser-client rollout risk

ECS rolling deployment has an explicit drain boundary. Browser JavaScript does not.

A user may keep a pre-F7 browser tab open after the compatibility Frontend has been deployed.

If 1.1 were activated immediately, that stale tab could start a new 1.1 Session but still attempt the old direct-pole UI if an exact tie is reached later.

For the current portfolio deployment, activation should therefore occur only after:

- CloudFront invalidation is complete;
- a fresh browser load is verified to serve the F7-compatible Frontend;
- a reasonable controlled compatibility window has passed.

For a larger public production system, stronger client-version enforcement or a more explicit minimum-client compatibility mechanism would be worth considering. That is not required for the current MVP.

## 9. Automatic CD verification

The repository has successful manual CD evidence, but GitHub Actions history currently contains no successful `workflow_run` automatic CD deployment; prior automatic-trigger runs were skipped by job conditions.

Therefore this compatibility release is still the intended first live verification of:

```text
merge to main
  -> CI success
  -> CD workflow_run
  -> Backend deploy
  -> Frontend deploy
  -> public smoke test
```

Before merge, confirm the repository variable/gate intended for this test enables automatic production CD.

The available GitHub connector cannot read repository Actions variables directly, so this gate requires a manual Settings check.

## 10. Portfolio README screenshot

The root README text is aligned to F7-D, but the existing:

```text
docs/assets/readme/ui-overview.png
```

was created before the new Landing hero and enriched Result view.

This does not block the compatibility release or activation mechanics.

For portfolio accuracy, refresh the README overview screenshot from the real deployed application after this release reaches AWS.

## 11. Merge strategy

The feature branch intentionally accumulated Backend 2A–2E, Frontend F7-A–F7-E, assets and documentation in one long-lived PR.

Do not preserve the large development commit sequence on `main`.

Use **Squash merge** so the completed feature lands as one coherent repository-history commit.

A suitable final squash title is:

```text
feat(assessment): add contextual tie-break DefinitionVersion 1.1 compatibility
```

Suggested body:

```text
- add immutable 1.1 contextual tie-break specification and persistence provenance
- preserve retained 1.0 direct-pole semantics and historical decision sources
- implement version-aware tie-break GET/PUT and finalization compatibility
- add React contextual tie-break flow with rolling-deploy compatibility
- enrich personality results with type/letter content and optimized artwork
- replace the Landing placeholder visual with the final hero artwork
- align OpenAPI, ADRs, architecture, roadmap, testing and release documentation
```

## 12. Final F7-E decision

```text
Backend compatibility               READY
Frontend 1.0 compatibility          READY
Frontend 1.1 contextual flow        READY
Result presentation                 READY
Landing presentation                READY
Tests / CI                          READY
Living documentation                READY
Compatibility release               READY

DefinitionVersion 1.1 activation    NOT IN THIS RELEASE
Automatic CD live verification      POST-MERGE CHECK
README UI screenshot refresh        POST-DEPLOY PORTFOLIO CHECK
```

The next engineering step after the compatibility release succeeds is the separate DefinitionVersion 1.1 activation change.
