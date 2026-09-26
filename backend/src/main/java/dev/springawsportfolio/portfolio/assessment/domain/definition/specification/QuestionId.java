package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record QuestionId(String value) {

    public QuestionId {
        Objects.requireNonNull(
                value,
                "QuestionId value must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "QuestionId value must not be blank"
            );
        }
    }
}
