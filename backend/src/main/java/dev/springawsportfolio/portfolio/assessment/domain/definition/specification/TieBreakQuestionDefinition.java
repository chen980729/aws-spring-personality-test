package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TieBreakQuestionDefinition(
        TieBreakQuestionId questionId,
        DimensionCode dimension,
        String instruction,
        String prompt,
        List<TieBreakOptionDefinition> options
) {

    public TieBreakQuestionDefinition {
        Objects.requireNonNull(
                questionId,
                "questionId must not be null"
        );
        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
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

        if (instruction.isBlank()) {
            throw new IllegalArgumentException(
                    "tie-break instruction must not be blank"
            );
        }

        if (prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "tie-break prompt must not be blank"
            );
        }

        if (options.size() != 2) {
            throw new IllegalArgumentException(
                    "tie-break question must contain exactly two options"
            );
        }

        List<TieBreakOptionDefinition> copiedOptions =
                List.copyOf(options);

        Set<TieBreakOptionId> optionIds =
                new HashSet<>();

        Set<PoleCode> resolvedPoles =
                new HashSet<>();

        for (TieBreakOptionDefinition option : copiedOptions) {
            if (!optionIds.add(option.optionId())) {
                throw new IllegalArgumentException(
                        "duplicate tie-break option id: "
                                + option.optionId().value()
                );
            }

            resolvedPoles.add(
                    option.resolvedPole()
            );
        }

        if (resolvedPoles.size() != 2) {
            throw new IllegalArgumentException(
                    "tie-break options must resolve to different poles"
            );
        }

        options = copiedOptions;
    }
}
