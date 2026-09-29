package dev.springawsportfolio.portfolio.assessment.domain.session;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.time.Instant;
import java.util.Objects;

public final class AssessmentSession {

    private final AssessmentSessionId id;

    private final UserId ownerUserId;

    private final AssessmentDefinitionId definitionId;

    private final AssessmentDefinitionVersionId
            definitionVersionId;

    private AssessmentSessionStatus status;

    private QuestionnaireResponse questionnaireResponse;

    private Instant questionnaireSubmittedAt;

    private InitialAssessmentResult initialResult;

    private FinalAssessmentResult finalResult;

    private final Instant createdAt;

    private Instant completedAt;

    private Instant abandonedAt;

    private AssessmentSession(
            AssessmentSessionId id,
            UserId ownerUserId,
            AssessmentDefinitionId definitionId,
            AssessmentDefinitionVersionId definitionVersionId,
            AssessmentSessionStatus status,
            QuestionnaireResponse questionnaireResponse,
            Instant questionnaireSubmittedAt,
            InitialAssessmentResult initialResult,
            FinalAssessmentResult finalResult,
            Instant createdAt,
            Instant completedAt,
            Instant abandonedAt
    ) {
        this.id = Objects.requireNonNull(
                id,
                "id must not be null"
        );

        this.ownerUserId = Objects.requireNonNull(
                ownerUserId,
                "ownerUserId must not be null"
        );

        this.definitionId = Objects.requireNonNull(
                definitionId,
                "definitionId must not be null"
        );

        this.definitionVersionId =
                Objects.requireNonNull(
                        definitionVersionId,
                        "definitionVersionId must not be null"
                );

        this.status = Objects.requireNonNull(
                status,
                "status must not be null"
        );

        this.questionnaireResponse =
                Objects.requireNonNull(
                        questionnaireResponse,
                        "questionnaireResponse must not be null"
                );

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        validateLifecycle(
                status,
                questionnaireSubmittedAt,
                initialResult,
                finalResult,
                createdAt,
                completedAt,
                abandonedAt
        );

        this.questionnaireSubmittedAt =
                questionnaireSubmittedAt;

        this.initialResult =
                initialResult;

        this.finalResult =
                finalResult;

        this.completedAt =
                completedAt;

        this.abandonedAt =
                abandonedAt;
    }

    public static AssessmentSession start(
            AssessmentSessionId id,
            UserId ownerUserId,
            AssessmentDefinitionId definitionId,
            AssessmentDefinitionVersionId definitionVersionId,
            Instant createdAt
    ) {
        return new AssessmentSession(
                id,
                ownerUserId,
                definitionId,
                definitionVersionId,
                AssessmentSessionStatus.IN_PROGRESS,
                QuestionnaireResponse.empty(),
                null,
                null,
                null,
                createdAt,
                null,
                null
        );
    }

    public static AssessmentSession restore(
            AssessmentSessionId id,
            UserId ownerUserId,
            AssessmentDefinitionId definitionId,
            AssessmentDefinitionVersionId definitionVersionId,
            AssessmentSessionStatus status,
            QuestionnaireResponse questionnaireResponse,
            Instant questionnaireSubmittedAt,
            InitialAssessmentResult initialResult,
            FinalAssessmentResult finalResult,
            Instant createdAt,
            Instant completedAt,
            Instant abandonedAt
    ) {
        return new AssessmentSession(
                id,
                ownerUserId,
                definitionId,
                definitionVersionId,
                status,
                questionnaireResponse,
                questionnaireSubmittedAt,
                initialResult,
                finalResult,
                createdAt,
                completedAt,
                abandonedAt
        );
    }

    public void replaceQuestionnaireResponse(
            QuestionnaireResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response must not be null"
        );

        if (
                status != AssessmentSessionStatus.IN_PROGRESS
                        || questionnaireSubmittedAt != null
        ) {
            throw new IllegalStateException(
                    "questionnaire response can only be replaced "
                            + "before submission"
            );
        }

        questionnaireResponse =
                response;
    }

    public AssessmentSubmissionOutcome submit(
            QuestionnaireResponse finalResponse,
            InitialAssessmentResult initialResult,
            FinalAssessmentResult immediateFinalResult,
            Instant submittedAt
    ) {
        Objects.requireNonNull(
                finalResponse,
                "finalResponse must not be null"
        );

        Objects.requireNonNull(
                initialResult,
                "initialResult must not be null"
        );

        Objects.requireNonNull(
                submittedAt,
                "submittedAt must not be null"
        );

        if (
                status
                        == AssessmentSessionStatus.ABANDONED
        ) {
            throw new IllegalStateException(
                    "abandoned assessment session "
                            + "cannot be submitted"
            );
        }

        if (
                status
                        == AssessmentSessionStatus.COMPLETED
                        || questionnaireSubmittedAt != null
        ) {
            throw new IllegalStateException(
                    "assessment questionnaire "
                            + "has already been submitted"
            );
        }

        if (
                status
                        != AssessmentSessionStatus.IN_PROGRESS
        ) {
            throw new IllegalStateException(
                    "only an IN_PROGRESS assessment session "
                            + "can be submitted"
            );
        }

        if (
                submittedAt.isBefore(
                        createdAt
                )
        ) {
            throw new IllegalArgumentException(
                    "submittedAt must not be before createdAt"
            );
        }

        boolean hasAmbiguity =
                initialResult
                        .hasAmbiguousDimensions();

        if (
                hasAmbiguity
                        && immediateFinalResult != null
        ) {
            throw new IllegalArgumentException(
                    "assessment with ambiguous dimensions "
                            + "must not have an immediate final result"
            );
        }

        if (
                !hasAmbiguity
                        && immediateFinalResult == null
        ) {
            throw new IllegalArgumentException(
                    "assessment without ambiguous dimensions "
                            + "must have an immediate final result"
            );
        }

        this.questionnaireResponse =
                finalResponse;

        this.questionnaireSubmittedAt =
                submittedAt;

        this.initialResult =
                initialResult;

        if (hasAmbiguity) {
            this.status =
                    AssessmentSessionStatus
                            .AWAITING_CLARIFICATION;

            this.finalResult =
                    null;

            this.completedAt =
                    null;

            return AssessmentSubmissionOutcome
                    .awaitingClarification(
                            initialResult
                                    .ambiguousDimensions()
                    );
        }

        this.finalResult =
                immediateFinalResult;

        this.status =
                AssessmentSessionStatus.COMPLETED;

        this.completedAt =
                submittedAt;

        return AssessmentSubmissionOutcome.completedSubmission();
    }

    public void beginClarification() {
        if (
                status
                        != AssessmentSessionStatus.AWAITING_CLARIFICATION
        ) {
            throw new IllegalStateException(
                    "only an AWAITING_CLARIFICATION session "
                            + "can begin clarification"
            );
        }

        status =
                AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS;
    }

    public void returnToAwaitingClarification() {
        if (
                status
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            throw new IllegalStateException(
                    "only a CLARIFICATION_IN_PROGRESS session "
                            + "can return to awaiting clarification"
            );
        }

        status =
                AssessmentSessionStatus.AWAITING_CLARIFICATION;
    }

    public void completeAfterClarification(
            FinalAssessmentResult finalResult,
            Instant completedAt
    ) {
        Objects.requireNonNull(
                finalResult,
                "finalResult must not be null"
        );

        Objects.requireNonNull(
                completedAt,
                "completedAt must not be null"
        );

        if (
                status
                        != AssessmentSessionStatus.AWAITING_CLARIFICATION
                        && status
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            throw new IllegalStateException(
                    "only an active post-submission clarification session "
                            + "can be completed after clarification"
            );
        }

        if (
                completedAt.isBefore(
                        questionnaireSubmittedAt
                )
        ) {
            throw new IllegalArgumentException(
                    "completedAt must not be before questionnaireSubmittedAt"
            );
        }

        this.finalResult =
                finalResult;

        status =
                AssessmentSessionStatus.COMPLETED;

        this.completedAt =
                completedAt;
    }

    public void abandon(
            Instant abandonedAt
    ) {
        Objects.requireNonNull(
                abandonedAt,
                "abandonedAt must not be null"
        );

        if (!status.isActive()) {
            throw new IllegalStateException(
                    "only an active assessment session "
                            + "can be abandoned"
            );
        }

        if (
                abandonedAt.isBefore(
                        createdAt
                )
        ) {
            throw new IllegalArgumentException(
                    "abandonedAt must not be before createdAt"
            );
        }

        status =
                AssessmentSessionStatus.ABANDONED;

        this.abandonedAt =
                abandonedAt;
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean isQuestionnaireSubmitted() {
        return questionnaireSubmittedAt != null;
    }

    public AssessmentSessionId id() {
        return id;
    }

    public UserId ownerUserId() {
        return ownerUserId;
    }

    public AssessmentDefinitionId definitionId() {
        return definitionId;
    }

    public AssessmentDefinitionVersionId
    definitionVersionId() {
        return definitionVersionId;
    }

    public AssessmentSessionStatus status() {
        return status;
    }

    public QuestionnaireResponse questionnaireResponse() {
        return questionnaireResponse;
    }

    public Instant questionnaireSubmittedAt() {
        return questionnaireSubmittedAt;
    }

    public InitialAssessmentResult initialResult() {
        return initialResult;
    }

    public FinalAssessmentResult finalResult() {
        return finalResult;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public Instant abandonedAt() {
        return abandonedAt;
    }

    private static void validateLifecycle(
            AssessmentSessionStatus status,
            Instant questionnaireSubmittedAt,
            InitialAssessmentResult initialResult,
            FinalAssessmentResult finalResult,
            Instant createdAt,
            Instant completedAt,
            Instant abandonedAt
    ) {
        if (
                questionnaireSubmittedAt != null
                        && questionnaireSubmittedAt
                        .isBefore(
                                createdAt
                        )
        ) {
            throw new IllegalArgumentException(
                    "questionnaireSubmittedAt "
                            + "must not be before createdAt"
            );
        }

        if (
                (questionnaireSubmittedAt == null)
                        != (initialResult == null)
        ) {
            throw new IllegalArgumentException(
                    "questionnaire submission fact and "
                            + "initial result must exist together"
            );
        }

        if (
                finalResult != null
                        && status
                        != AssessmentSessionStatus.COMPLETED
        ) {
            throw new IllegalArgumentException(
                    "final result may exist only "
                            + "for a COMPLETED session"
            );
        }

        switch (status) {
            case IN_PROGRESS -> {
                if (
                        questionnaireSubmittedAt != null
                                || initialResult != null
                                || finalResult != null
                ) {
                    throw new IllegalArgumentException(
                            "IN_PROGRESS session must not have "
                                    + "submission or result facts"
                    );
                }

                requireNoTerminalTimestamps(
                        completedAt,
                        abandonedAt
                );
            }

            case AWAITING_CLARIFICATION,
                 CLARIFICATION_IN_PROGRESS -> {

                if (
                        questionnaireSubmittedAt == null
                                || initialResult == null
                ) {
                    throw new IllegalArgumentException(
                            "post-submission active session "
                                    + "must have submission fact "
                                    + "and initial result"
                    );
                }

                if (finalResult != null) {
                    throw new IllegalArgumentException(
                            "active clarification session "
                                    + "must not have a final result"
                    );
                }

                requireNoTerminalTimestamps(
                        completedAt,
                        abandonedAt
                );
            }

            case COMPLETED -> {
                if (
                        questionnaireSubmittedAt == null
                                || initialResult == null
                ) {
                    throw new IllegalArgumentException(
                            "completed session must have "
                                    + "submission fact and initial result"
                    );
                }

                if (finalResult == null) {
                    throw new IllegalArgumentException(
                            "completed session must have "
                                    + "a final result"
                    );
                }

                if (completedAt == null) {
                    throw new IllegalArgumentException(
                            "completed session "
                                    + "must have completedAt"
                    );
                }

                if (abandonedAt != null) {
                    throw new IllegalArgumentException(
                            "completed session "
                                    + "must not have abandonedAt"
                    );
                }

                if (
                        completedAt.isBefore(
                                createdAt
                        )
                ) {
                    throw new IllegalArgumentException(
                            "completedAt must not be before createdAt"
                    );
                }
            }

            case ABANDONED -> {
                if (finalResult != null) {
                    throw new IllegalArgumentException(
                            "abandoned session "
                                    + "must not have final result"
                    );
                }

                if (abandonedAt == null) {
                    throw new IllegalArgumentException(
                            "abandoned session "
                                    + "must have abandonedAt"
                    );
                }

                if (completedAt != null) {
                    throw new IllegalArgumentException(
                            "abandoned session "
                                    + "must not have completedAt"
                    );
                }

                if (
                        abandonedAt.isBefore(
                                createdAt
                        )
                ) {
                    throw new IllegalArgumentException(
                            "abandonedAt must not be before createdAt"
                    );
                }
            }
        }
    }

    private static void requireNoTerminalTimestamps(
            Instant completedAt,
            Instant abandonedAt
    ) {
        if (
                completedAt != null
                        || abandonedAt != null
        ) {
            throw new IllegalArgumentException(
                    "active session must not have "
                            + "completedAt or abandonedAt"
            );
        }
    }
}