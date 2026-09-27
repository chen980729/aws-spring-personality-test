package dev.springawsportfolio.portfolio.assessment.application.query.history;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AssessmentHistoryItem(
        UUID sessionId,
        String assessmentCode,
        String assessmentVersion,
        String finalType,
        Instant completedAt
) {

    public AssessmentHistoryItem {
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        Objects.requireNonNull(
                assessmentCode,
                "assessmentCode must not be null"
        );

        if (assessmentCode.isBlank()) {
            throw new IllegalArgumentException(
                    "assessmentCode must not be blank"
            );
        }

        Objects.requireNonNull(
                assessmentVersion,
                "assessmentVersion must not be null"
        );

        if (assessmentVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "assessmentVersion must not be blank"
            );
        }

        Objects.requireNonNull(
                finalType,
                "finalType must not be null"
        );

        if (finalType.isBlank()) {
            throw new IllegalArgumentException(
                    "finalType must not be blank"
            );
        }

        Objects.requireNonNull(
                completedAt,
                "completedAt must not be null"
        );
    }
}
