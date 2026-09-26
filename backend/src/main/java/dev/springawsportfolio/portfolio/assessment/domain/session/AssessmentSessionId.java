package dev.springawsportfolio.portfolio.assessment.domain.session;

import java.util.Objects;
import java.util.UUID;

public record AssessmentSessionId(UUID value) {

    public AssessmentSessionId {
        Objects.requireNonNull(
                value,
                "AssessmentSessionId value must not be null"
        );
    }

    public static AssessmentSessionId newId() {
        return new AssessmentSessionId(
                UUID.randomUUID()
        );
    }
}
