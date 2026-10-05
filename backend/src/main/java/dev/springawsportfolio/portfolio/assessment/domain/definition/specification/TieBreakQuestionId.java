package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record TieBreakQuestionId(String value) {

    public TieBreakQuestionId {
        Objects.requireNonNull(
                value,
                "TieBreakQuestionId value must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "TieBreakQuestionId value must not be blank"
            );
        }
    }
}
