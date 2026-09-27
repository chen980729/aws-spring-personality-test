# Product Requirements

## 1. Product Goal

Build a production-oriented personal full-stack cloud portfolio project that demonstrates modern web development, backend/domain design, cloud deployment, CI/CD, Infrastructure as Code, testing, and the ability to explain engineering trade-offs in interviews.

The product is a non-clinical personality assessment website. Its main differentiator is an optional AI-assisted clarification flow for dimensions whose deterministic questionnaire result is ambiguous, plus group-based result sharing for mutual understanding.

## 2. Technology Baseline

- Frontend: React + TypeScript
- Backend: Java 21 + Spring Boot
- Database: PostgreSQL
- Cloud: AWS
- Container: Docker
- CI/CD: GitHub Actions
- Infrastructure as Code: Terraform
- Version control: Git + GitHub

Detailed service selection, physical database design, REST shape, Spring classes/packages, and AWS topology are owned by specialist design workstreams and are not duplicated in this requirements document.

## 3. Users and Roles

### Registered User

A registered user can authenticate, take assessments, resume unfinished assessments, use optional AI clarification, view and delete their own assessment history, create or join groups, and control whether assessment results are shared with groups.

### Group Admin

The creator becomes the first Admin and first active Member. Creator is an immutable historical fact, not a permanent role. Admin authority can be transferred to another active Member without recipient confirmation in MVP. The current Admin cannot leave directly; they must first transfer Admin authority or disband the Group.

The Admin can manage join requests, invite people by sharing the group code, transfer Admin authority, and disband the Group. MVP does not require email-based invitations.

## 4. Final MVP Scope

The final MVP includes both the core assessment vertical slice and the group/sharing feature set. Delivery may be staged, but Group remains part of the MVP completion target.

### 4.1 Identity and Authentication

- Register with email, password, and a non-sensitive human-readable display name.
- Login.
- Logout.
- Retrieve the authenticated user's basic identity context, including display name.
- Password reset is optional/deferred unless time allows.

### 4.2 Assessment Catalog

- Show available assessments.
- MVP implements one executable assessment: Sixteen Personality.
- The architecture must allow additional assessment definitions, such as Big Five, without requiring microservices.

### 4.3 Sixteen Personality Assessment Flow

1. User views assessment introduction.
2. User starts an assessment attempt. If an active attempt already exists, the normal start flow resumes it. The user may explicitly choose **Start New**, which atomically abandons the identified active attempt and creates a replacement attempt.
3. The system binds the attempt to the unique currently `AVAILABLE` `AssessmentDefinitionVersion` for that AssessmentDefinition.
4. User answers the questionnaire; progress can be saved for resume.
5. User submits the questionnaire.
6. Submission freezes the response for that attempt.
7. Backend performs deterministic scoring and ambiguity evaluation.
8. If no dimension is ambiguous, the backend finalizes the assessment.
9. If one or more dimensions are ambiguous, the user can use AI-assisted clarification, skip the current dimension, or decline all remaining AI clarification.
10. After every ambiguous dimension is clarified or skipped, the backend creates the final result and completes the session.

### 4.4 AI-Assisted Clarification

AI clarification is optional and non-clinical. It is intended to help the user reflect on ambiguous dimensions through examples, follow-up questions, and contextual discussion.

MVP requirements:

- Clarification is limited to dimensions already classified as ambiguous by deterministic assessment logic.
- AI cannot modify the immutable `InitialAssessmentResult`.
- AI does not control assessment lifecycle transitions or final business decisions.
- AI output must not be presented as a clinical diagnosis or as scientifically guaranteed to be more accurate unless validated evidence exists.
- AI clarification must not invent a new mathematical percentage merely from conversation.
- Clarification is sequential within one AssessmentSession in MVP: at most one `DimensionClarification` may be `IN_PROGRESS` at a time; parallel clarification conversations are not supported.
- The full AI conversation transcript is not persisted in MVP.
- Only an accepted `ClarificationResult`, required AI provenance, and an optional necessary summary become durable assessment history.
- Technical AI failure enters `FAILED_RETRYABLE`; the user may explicitly retry that same clarification lifecycle or skip it.
- If the temporary conversation context is lost before an accepted result is persisted, the user does not resume from the exact message turn. The affected clarification can restart from its clarification-ready/retryable state.
- If the parent AssessmentSession is abandoned while an external AI call is in flight, any late result is discarded after reloading/revalidating the Session.

### 4.5 Assessment History

- User can list completed assessment attempts.
- User can view the details of a historical assessment.
- A completed assessment's contents are immutable: answers and accepted results cannot be edited in place.
- The owner can delete a historical assessment record.

Immutability governs modification, not deletion. MVP historical-assessment deletion is a hard delete of the owned AssessmentSession and its Session-dependent persisted Assessment data after Group orchestration ends any ACTIVE Shares that reference it; ended Group share/consent history is retained without retaining the private Assessment content.

### 4.6 Group and Sharing

Final MVP includes:

- Create a group.
- Group name and group code; Group name is immutable in MVP. Avatar is optional/deferred.
- Creator becomes admin.
- User can request to join a group using its group code.
- Admin can approve or reject join requests.
- Admin can invite people by sharing the group code; email invitation is not required.
- Members can leave according to group rules; the current Admin must transfer Admin authority or disband before leaving.
- Admin can transfer Admin authority to another active Member and can disband the group.
- Each active Membership may explicitly share at most one selected `COMPLETED AssessmentSession` result with its Group at a time.
- Members can view only the explicitly allowed shareable representation of another member's currently shared result. Group read models may include that member's Identity-owned `displayName`, but not their email.

Group membership does not imply consent to assessment-data sharing. Default sharing is **NOT SHARED**. Sharing consent is owned by the Group side as `GroupAssessmentShare`, bound to one Membership and one specific completed AssessmentSession.

Joining a group must not automatically expose:

- `QuestionnaireResponse` or individual answers.
- Complete assessment history.
- AI conversation content.
- Private clarification context.
- Account or authentication information.


## 5. Core Business Rules

- One active `AssessmentSession` per `User + AssessmentDefinition`.
- This invariant must be enforceable from durable system state and survive application restart; it cannot rely only on frontend checks or in-memory state.
- A session binds one exact `AssessmentDefinitionVersion` and never auto-upgrades.
- For one `AssessmentDefinition`, at most one `AssessmentDefinitionVersion` may be `AVAILABLE` in MVP; normal Start Assessment therefore resolves one unique executable version.
- Once a definition version becomes executable/available to users, its assessment specification is immutable.
- `IN_PROGRESS`, `AWAITING_CLARIFICATION`, and `CLARIFICATION_IN_PROGRESS` may transition to terminal `ABANDONED`; `COMPLETED` and `ABANDONED` cannot be abandoned again.
- Questionnaire submit is an irreversible business boundary for the response in that attempt. Submission plus deterministic scoring/ambiguity setup/finalization is one atomic local transaction in MVP.
- `InitialAssessmentResult` is deterministic evidence and cannot be rewritten by AI clarification.
- Only ambiguous dimensions are eligible for clarification.
- One logical clarification lifecycle exists per `Session + Dimension`.
- At most one clarification lifecycle may be actively `IN_PROGRESS` in a Session at a time for MVP.
- All ambiguous dimensions must be `CLARIFIED` or `SKIPPED` before completion.
- Completed assessment content is immutable, but the owner may delete the historical record.

## 6. Privacy and Data-Minimization Principles

- Collect and retain only data required for product behavior, traceability, security, or agreed operational needs.
- Full AI conversation transcripts are not authoritative assessment history and are not persisted in MVP.
- Group sharing is explicit and defaults to not shared.
- Group members must never receive private questionnaire answers, private AI context, complete assessment history, or account/authentication data merely because they share group membership.
- Historical traceability and explainability are required. Strict reproducibility is required only for deterministic assessment components.

## 7. Non-Goals for MVP

- Clinical diagnosis or medical/psychological treatment claims.
- Microservice decomposition.
- Event Sourcing.
- Persisting complete LLM request/response or conversation history.
- Exact mid-conversation AI resume after browser/server/runtime-context loss.
- Email-based group invitation.
- Multiple additional assessment types such as Big Five.
- A centralized IAM/authorization platform.
- Advanced organization/team administration.

## 8. Open Product / Architecture Questions

The following remain intentionally open or deferred after Domain/Backend alignment:

- Detailed delete-account policy. Account deletion is deferred from Core MVP; current persistence intentionally prevents direct account-row deletion until a dedicated deletion/anonymization policy exists.
- Exact temporary multi-turn AI runtime-context mechanism (memory/cache/temporary persistence/provider context/client-supplied context).

## 9. Definition of Done for Final MVP

The project is considered MVP-complete when:

- The application runs end to end.
- Core authentication works.
- Sixteen Personality has a documented executable specification and deterministic scoring.
- Ambiguity and optional AI clarification work with defined failure/recovery behavior.
- Assessment history can be viewed and individual historical assessments can be deleted.
- Group membership and explicit result sharing work.
- Backend business invariants are tested.
- Frontend has usable flows for assessment, clarification, result, history, deletion, and groups.
- PostgreSQL persistence is implemented with appropriate integrity/recovery semantics.
- Docker-based local execution is available.
- The system is deployable to AWS.
- CI/CD is implemented with GitHub Actions.
- AWS infrastructure is represented through Terraform where appropriate.
- README and architecture/domain documentation explain the main decisions and trade-offs.
