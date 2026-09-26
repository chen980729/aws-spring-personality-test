package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record QuestionDefinition(
        QuestionId questionId,
        int position,
        String prompt,
        DimensionCode dimension,
        PoleCode keyedPole
) {

    public QuestionDefinition {
        Objects.requireNonNull(
                questionId,
                "questionId must not be null"
        );
        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
        );
        Objects.requireNonNull(
                keyedPole,
                "keyedPole must not be null"
        );
        Objects.requireNonNull(
                prompt,
                "prompt must not be null"
        );

        if (position <= 0) {
            throw new IllegalArgumentException(
                    "question position must be positive"
            );
        }

        if (prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "question prompt must not be blank"
            );
        }
    }
}