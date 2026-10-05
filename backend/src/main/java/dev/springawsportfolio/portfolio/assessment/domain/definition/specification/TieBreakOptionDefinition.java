package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record TieBreakOptionDefinition(
        TieBreakOptionId optionId,
        String text,
        PoleCode resolvedPole
) {

    public TieBreakOptionDefinition {
        Objects.requireNonNull(
                optionId,
                "optionId must not be null"
        );
        Objects.requireNonNull(
                text,
                "text must not be null"
        );
        Objects.requireNonNull(
                resolvedPole,
                "resolvedPole must not be null"
        );

        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    "tie-break option text must not be blank"
            );
        }
    }
}
