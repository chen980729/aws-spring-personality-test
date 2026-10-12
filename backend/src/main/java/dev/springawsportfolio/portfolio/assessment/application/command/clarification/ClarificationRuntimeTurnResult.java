package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;

import java.util.Objects;

public record ClarificationRuntimeTurnResult(
        Status status,
        String followUpQuestion,
        AssessmentSessionResult session
) {

    public enum Status {
        FOLLOW_UP_REQUIRED,
        JUDGEMENT_ACCEPTED,
        FAILED_RETRYABLE,
        STALE_DISCARDED
    }

    public ClarificationRuntimeTurnResult {
        Objects.requireNonNull(
                status,
                "status must not be null"
        );

        switch (status) {
            case FOLLOW_UP_REQUIRED -> {
                if (
                        followUpQuestion == null
                                || followUpQuestion.isBlank()
                ) {
                    throw new IllegalArgumentException(
                            "FOLLOW_UP_REQUIRED must contain question"
                    );
                }

                if (session != null) {
                    throw new IllegalArgumentException(
                            "FOLLOW_UP_REQUIRED must not contain session"
                    );
                }
            }

            case JUDGEMENT_ACCEPTED,
                 FAILED_RETRYABLE -> {
                if (followUpQuestion != null) {
                    throw new IllegalArgumentException(
                            "terminal runtime result must not contain question"
                    );
                }

                Objects.requireNonNull(
                        session,
                        "accepted runtime result must contain session"
                );
            }

            case STALE_DISCARDED -> {
                if (
                        followUpQuestion != null
                                || session != null
                ) {
                    throw new IllegalArgumentException(
                            "STALE_DISCARDED must not contain payload"
                    );
                }
            }
        }
    }

    public static ClarificationRuntimeTurnResult followUp(
            String question
    ) {
        return new ClarificationRuntimeTurnResult(
                Status.FOLLOW_UP_REQUIRED,
                question,
                null
        );
    }

    public static ClarificationRuntimeTurnResult judgementAccepted(
            AssessmentSessionResult session
    ) {
        return new ClarificationRuntimeTurnResult(
                Status.JUDGEMENT_ACCEPTED,
                null,
                session
        );
    }

    public static ClarificationRuntimeTurnResult failedRetryable(
            AssessmentSessionResult session
    ) {
        return new ClarificationRuntimeTurnResult(
                Status.FAILED_RETRYABLE,
                null,
                session
        );
    }

    public static ClarificationRuntimeTurnResult staleDiscarded() {
        return new ClarificationRuntimeTurnResult(
                Status.STALE_DISCARDED,
                null,
                null
        );
    }
}
