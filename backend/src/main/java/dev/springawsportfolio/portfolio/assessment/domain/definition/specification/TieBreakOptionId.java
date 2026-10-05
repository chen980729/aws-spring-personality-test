package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record TieBreakOptionId(String value) {

    public TieBreakOptionId {
        Objects.requireNonNull(
                value,
                "TieBreakOptionId value must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "TieBreakOptionId value must not be blank"
            );
        }
    }
}
