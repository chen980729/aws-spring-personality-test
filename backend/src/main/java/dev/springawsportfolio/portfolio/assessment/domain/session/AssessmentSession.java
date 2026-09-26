package dev.springawsportfolio.portfolio.assessment.domain.session;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
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
                createdAt,
                completedAt,
                abandonedAt
        );

        this.questionnaireSubmittedAt =
                questionnaireSubmittedAt;

        this.completedAt = completedAt;
        this.abandonedAt = abandonedAt;
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

        questionnaireResponse = response;
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

        if (abandonedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "abandonedAt must not be before createdAt"
            );
        }

        status =
                AssessmentSessionStatus.ABANDONED;

        this.abandonedAt = abandonedAt;
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
            Instant createdAt,
            Instant completedAt,
            Instant abandonedAt
    ) {
        if (
                questionnaireSubmittedAt != null
                        && questionnaireSubmittedAt.isBefore(
                        createdAt
                )
        ) {
            throw new IllegalArgumentException(
                    "questionnaireSubmittedAt "
                            + "must not be before createdAt"
            );
        }

        switch (status) {
            case IN_PROGRESS -> {
                if (questionnaireSubmittedAt != null) {
                    throw new IllegalArgumentException(
                            "IN_PROGRESS session "
                                    + "must not be submitted"
                    );
                }

                requireNoTerminalTimestamps(
                        completedAt,
                        abandonedAt
                );
            }

            case AWAITING_CLARIFICATION,
                 CLARIFICATION_IN_PROGRESS -> {

                if (questionnaireSubmittedAt == null) {
                    throw new IllegalArgumentException(
                            "post-submission active session "
                                    + "must have questionnaireSubmittedAt"
                    );
                }

                requireNoTerminalTimestamps(
                        completedAt,
                        abandonedAt
                );
            }

            case COMPLETED -> {
                if (questionnaireSubmittedAt == null) {
                    throw new IllegalArgumentException(
                            "completed session "
                                    + "must have questionnaireSubmittedAt"
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

                if (completedAt.isBefore(createdAt)) {
                    throw new IllegalArgumentException(
                            "completedAt must not be before createdAt"
                    );
                }
            }

            case ABANDONED -> {
                /*
                 * ABANDONED is legal both before and after
                 * questionnaire submission.
                 */
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

                if (abandonedAt.isBefore(createdAt)) {
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
