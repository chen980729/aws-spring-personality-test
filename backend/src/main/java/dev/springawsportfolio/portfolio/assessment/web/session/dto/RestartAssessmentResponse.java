package dev.springawsportfolio.portfolio.assessment.web.session.dto;

import java.util.Objects;
import java.util.UUID;

public record RestartAssessmentResponse(
        UUID abandonedSessionId,
        AssessmentSessionResponse session
) {

    public RestartAssessmentResponse {
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