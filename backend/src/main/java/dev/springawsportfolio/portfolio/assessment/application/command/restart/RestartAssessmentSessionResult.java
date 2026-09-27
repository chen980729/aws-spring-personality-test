package dev.springawsportfolio.portfolio.assessment.application.command.restart;

import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;

import java.util.Objects;
import java.util.UUID;

public record RestartAssessmentSessionResult(
        UUID abandonedSessionId,
        AssessmentSessionResult session
) {

    public RestartAssessmentSessionResult {
        Objects.requireNonNull(
                abandonedSessionId,
                "abandonedSessionId must not be null"
        );

        Objects.requireNonNull(
                session,
                "session must not be null"
        );
    }
}
