package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;

import java.util.Objects;

public record ClarificationRuntimeTurnResult(
        Status status,
        Integer assistantSequence,
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
                        assistantSequence == null
                                || assistantSequence <= 0
                                || assistantSequence % 2 == 0
                ) {
                    throw new IllegalArgumentException(
                            "FOLLOW_UP_REQUIRED must contain positive odd assistantSequence"
                    );
                }

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
                if (
                        assistantSequence != null
                                || followUpQuestion != null
                ) {
                    throw new IllegalArgumentException(
                            "terminal runtime result must not contain follow-up payload"
                    );
                }

                Objects.requireNonNull(
                        session,
                        "accepted runtime result must contain session"
                );
            }

            case STALE_DISCARDED -> {
                if (
                        assistantSequence != null
                                || followUpQuestion != null
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
            int assistantSequence,
            String question
    ) {
        return new ClarificationRuntimeTurnResult(
                Status.FOLLOW_UP_REQUIRED,
                assistantSequence,
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
                null,
                session
        );
    }

    public static ClarificationRuntimeTurnResult staleDiscarded() {
        return new ClarificationRuntimeTurnResult(
                Status.STALE_DISCARDED,
                null,
                null,
                null
        );
    }
}
