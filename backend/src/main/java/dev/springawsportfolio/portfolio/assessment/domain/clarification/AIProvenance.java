package dev.springawsportfolio.portfolio.assessment.domain.clarification;

import java.util.Objects;

public record AIProvenance(
        String provider,
        String modelIdentifier,
        String clarificationPolicyRevision
) {

    public AIProvenance {
        requireNonBlank(
                provider,
                "provider"
        );

        requireNonBlank(
                modelIdentifier,
                "modelIdentifier"
        );

        requireNonBlank(
                clarificationPolicyRevision,
                "clarificationPolicyRevision"
        );
    }

    private static void requireNonBlank(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
    }
}
