package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record ScoringPolicy(
        ScoringPolicyType type,
        String revision,
        int answerCenter,
        int minimumAnswer,
        int maximumAnswer,
        int itemsPerDimension
) {

    public ScoringPolicy {
        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        Objects.requireNonNull(
                revision,
                "revision must not be null"
        );

        if (revision.isBlank()) {
            throw new IllegalArgumentException(
                    "revision must not be blank"
            );
        }

        if (minimumAnswer >= maximumAnswer) {
            throw new IllegalArgumentException(
                    "minimumAnswer must be less than maximumAnswer"
            );
        }

        if (
                answerCenter < minimumAnswer
                        || answerCenter > maximumAnswer
        ) {
            throw new IllegalArgumentException(
                    "answerCenter must be within answer range"
            );
        }

        if (itemsPerDimension <= 0) {
            throw new IllegalArgumentException(
                    "itemsPerDimension must be positive"
            );
        }

        if (
                type == ScoringPolicyType.CENTERED_BALANCED_KEYING
                        && answerCenter - minimumAnswer
                        != maximumAnswer - answerCenter
        ) {
            throw new IllegalArgumentException(
                    "CENTERED_BALANCED_KEYING requires "
                            + "a symmetric answer range"
            );
        }
    }

    public int maximumAbsoluteRawScore() {
        int maximumDistanceFromCenter = Math.max(
                maximumAnswer - answerCenter,
                answerCenter - minimumAnswer
        );

        return itemsPerDimension * maximumDistanceFromCenter;
    }
}