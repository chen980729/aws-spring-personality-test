package dev.springawsportfolio.portfolio.assessment.application.query.tiebreak;

import java.util.List;
import java.util.Objects;

public sealed interface DimensionTieBreakInteractionResult
        permits DimensionTieBreakInteractionResult.DirectPoleSelection,
        DimensionTieBreakInteractionResult.ContextualQuestion {

    String dimensionCode();

    record DirectPoleSelection(
            String dimensionCode,
            List<String> allowedPoles
    ) implements DimensionTieBreakInteractionResult {

        public DirectPoleSelection {
            Objects.requireNonNull(
                    dimensionCode,
                    "dimensionCode must not be null"
            );
            Objects.requireNonNull(
                    allowedPoles,
                    "allowedPoles must not be null"
            );

            allowedPoles =
                    List.copyOf(
                            allowedPoles
                    );

            if (allowedPoles.size() != 2) {
                throw new IllegalArgumentException(
                        "allowedPoles must contain exactly two values"
                );
            }
        }
    }

    record ContextualQuestion(
            String dimensionCode,
            String questionId,
            String instruction,
            String prompt,
            List<Option> options
    ) implements DimensionTieBreakInteractionResult {

        public ContextualQuestion {
            Objects.requireNonNull(
                    dimensionCode,
                    "dimensionCode must not be null"
            );
            Objects.requireNonNull(
                    questionId,
                    "questionId must not be null"
            );
            Objects.requireNonNull(
                    instruction,
                    "instruction must not be null"
            );
            Objects.requireNonNull(
                    prompt,
                    "prompt must not be null"
            );
            Objects.requireNonNull(
                    options,
                    "options must not be null"
            );

            options =
                    List.copyOf(
                            options
                    );

            if (options.size() != 2) {
                throw new IllegalArgumentException(
                        "contextual interaction must contain exactly two options"
                );
            }
        }
    }

    record Option(
            String optionId,
            String text
    ) {

        public Option {
            Objects.requireNonNull(
                    optionId,
                    "optionId must not be null"
            );
            Objects.requireNonNull(
                    text,
                    "text must not be null"
            );
        }
    }
}
