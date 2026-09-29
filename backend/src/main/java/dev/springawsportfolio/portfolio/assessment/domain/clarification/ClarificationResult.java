package dev.springawsportfolio.portfolio.assessment.domain.clarification;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;

import java.util.Objects;

public record ClarificationResult(
        ClarificationResolution resolution,
        PoleCode suggestedPole,
        ClarificationConfidence confidence,
        String reasoningSummary
) {

    public ClarificationResult {
        Objects.requireNonNull(
                resolution,
                "resolution must not be null"
        );

        Objects.requireNonNull(
                confidence,
                "confidence must not be null"
        );

        if (
                resolution == ClarificationResolution.RESOLVED
                        && suggestedPole == null
        ) {
            throw new IllegalArgumentException(
                    "RESOLVED clarification result must have suggestedPole"
            );
        }

        if (
                resolution == ClarificationResolution.UNCLEAR
                        && suggestedPole != null
        ) {
            throw new IllegalArgumentException(
                    "UNCLEAR clarification result must not have suggestedPole"
            );
        }

        if (
                reasoningSummary != null
                        && reasoningSummary.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "reasoningSummary must be null or non-blank"
            );
        }
    }
}
