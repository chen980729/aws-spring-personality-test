package dev.springawsportfolio.portfolio.assessment.domain.session;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;

import java.util.List;
import java.util.Objects;

public record AssessmentSubmissionOutcome(
        boolean completed,
        List<DimensionCode> ambiguousDimensions
) {

    public AssessmentSubmissionOutcome {
        Objects.requireNonNull(
                ambiguousDimensions,
                "ambiguousDimensions must not be null"
        );

        ambiguousDimensions =
                List.copyOf(
                        ambiguousDimensions
                );

        if (
                completed
                        && !ambiguousDimensions.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "completed submission must not have "
                            + "unresolved ambiguous dimensions"
            );
        }

        if (
                !completed
                        && ambiguousDimensions.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "non-completed submission must have "
                            + "ambiguous dimensions"
            );
        }
    }

    public static AssessmentSubmissionOutcome
    completedSubmission() {
        return new AssessmentSubmissionOutcome(
                true,
                List.of()
        );
    }

    public static AssessmentSubmissionOutcome
    awaitingClarification(
            List<DimensionCode> ambiguousDimensions
    ) {
        return new AssessmentSubmissionOutcome(
                false,
                ambiguousDimensions
        );
    }
}
