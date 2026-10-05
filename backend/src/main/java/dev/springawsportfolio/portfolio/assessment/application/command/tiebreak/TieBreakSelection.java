package dev.springawsportfolio.portfolio.assessment.application.command.tiebreak;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakOptionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakQuestionId;

import java.util.Objects;

public sealed interface TieBreakSelection
        permits TieBreakSelection.DirectPoleSelection,
        TieBreakSelection.ContextualOptionSelection {

    record DirectPoleSelection(
            PoleCode selectedPole
    ) implements TieBreakSelection {

        public DirectPoleSelection {
            Objects.requireNonNull(
                    selectedPole,
                    "selectedPole must not be null"
            );
        }
    }

    record ContextualOptionSelection(
            TieBreakQuestionId questionId,
            TieBreakOptionId selectedOptionId
    ) implements TieBreakSelection {

        public ContextualOptionSelection {
            Objects.requireNonNull(
                    questionId,
                    "questionId must not be null"
            );

            Objects.requireNonNull(
                    selectedOptionId,
                    "selectedOptionId must not be null"
            );
        }
    }
}
