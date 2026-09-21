# ADR-0002: Bind AssessmentSession to an Exact AssessmentDefinitionVersion

- Status: Accepted
- Date: 2026-09-15

## Context

Historical assessment interpretation requires knowing the exact questionnaire and deterministic rules used by an attempt.

## Decision

Every `AssessmentSession` binds one exact `AssessmentDefinitionVersion` at creation and never auto-upgrades. Once a version becomes `AVAILABLE`, its assessment specification is immutable. MVP permits at most one `AVAILABLE AssessmentDefinitionVersion` per `AssessmentDefinition`, so normal Start Assessment resolves one unique executable version without client version selection. Semantic changes require a new version.

## Consequences

Historical deterministic traceability is preserved and in-progress sessions cannot silently change behavior. Version lifecycle uses `DRAFT / AVAILABLE / RETIRED` in MVP. Publishing a new semantic version retires the previous available version and activates the new one without changing historical Session bindings.
