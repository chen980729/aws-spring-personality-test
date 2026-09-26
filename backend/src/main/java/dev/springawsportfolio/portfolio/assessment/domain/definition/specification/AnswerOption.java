package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record AnswerOption(
        int value,
        String label
) {

    public AnswerOption {
        Objects.requireNonNull(label, "label must not be null");

        if (label.isBlank()) {
            throw new IllegalArgumentException(
                    "answer option label must not be blank"
            );
        }
    }
}