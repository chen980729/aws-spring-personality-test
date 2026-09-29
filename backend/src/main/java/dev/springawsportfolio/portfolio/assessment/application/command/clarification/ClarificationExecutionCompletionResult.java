package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;

import java.util.Objects;

public record ClarificationExecutionCompletionResult(
        ClarificationExecutionCompletionStatus status,
        AssessmentSessionResult session
) {

    public ClarificationExecutionCompletionResult {
        Objects.requireNonNull(
                status,
                "status must not be null"
        );

        if (
                status == ClarificationExecutionCompletionStatus.ACCEPTED
                        && session == null
        ) {
            throw new IllegalArgumentException(
                    "accepted clarification completion must contain session"
            );
        }

        if (
                status == ClarificationExecutionCompletionStatus.STALE_DISCARDED
                        && session != null
        ) {
            throw new IllegalArgumentException(
                    "stale clarification completion must not contain session"
            );
        }
    }

    public static ClarificationExecutionCompletionResult accepted(
            AssessmentSessionResult session
    ) {
        return new ClarificationExecutionCompletionResult(
                ClarificationExecutionCompletionStatus.ACCEPTED,
                Objects.requireNonNull(
                        session,
                        "session must not be null"
                )
        );
    }

    public static ClarificationExecutionCompletionResult staleDiscarded() {
        return new ClarificationExecutionCompletionResult(
                ClarificationExecutionCompletionStatus.STALE_DISCARDED,
                null
        );
    }

    public boolean accepted() {
        return status
                == ClarificationExecutionCompletionStatus.ACCEPTED;
    }
}
