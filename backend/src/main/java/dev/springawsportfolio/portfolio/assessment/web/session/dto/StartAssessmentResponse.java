package dev.springawsportfolio.portfolio.assessment.web.session.dto;

import java.util.Objects;

public record StartAssessmentResponse(
        boolean created,
        AssessmentSessionResponse session
) {

    public StartAssessmentResponse {
        Objects.requireNonNull(
                session,
                "session must not be null"
        );
    }
}