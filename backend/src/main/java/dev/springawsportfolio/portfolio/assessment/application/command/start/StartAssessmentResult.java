package dev.springawsportfolio.portfolio.assessment.application.command.start;

import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;

import java.util.Objects;

public record StartAssessmentResult(
        boolean created,
        AssessmentSessionResult session
) {

    public StartAssessmentResult {
        Objects.requireNonNull(
                session,
                "session must not be null"
        );
    }
}