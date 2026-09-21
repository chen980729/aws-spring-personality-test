# ADR-0012: Hard Delete Historical Assessment Content While Preserving Group Share History

- Status: Accepted
- Date: 2026-09-20

## Context

The Assessment owner may delete historical private Assessment content, while Group must retain historical consent/share records. A normal foreign key from Group Share to AssessmentSession conflicts with these two requirements.

## Decision

Historical AssessmentSession deletion is a coordinated hard delete. Before deletion, active Group shares referencing the Session are ended with `ASSESSMENT_DELETED`. Session-owned child rows cascade with the Session.

`group_assessment_shares.assessment_session_id` intentionally has no database FK to `assessment_sessions`; ended Share history retains the old UUID as an opaque historical reference.

## Consequences

### Benefits

- Deleted private Assessment content is actually removed.
- Group consent history remains auditable within product needs.
- No soft-deleted answer/AI content is retained solely for ORM convenience.

### Costs

- Cross-domain active-reference integrity must be protected by orchestration, transaction and locking rather than a simple FK.
- Some historical Share references intentionally point to no current Assessment row.
