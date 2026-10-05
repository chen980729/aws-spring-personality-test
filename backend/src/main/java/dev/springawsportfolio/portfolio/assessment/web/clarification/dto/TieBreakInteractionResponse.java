package dev.springawsportfolio.portfolio.assessment.web.clarification.dto;

import java.util.List;

public sealed interface TieBreakInteractionResponse
        permits TieBreakInteractionResponse.DirectPoleSelection,
        TieBreakInteractionResponse.ContextualQuestion {

    record DirectPoleSelection(
            String interactionType,
            String dimensionCode,
            List<String> allowedPoles
    ) implements TieBreakInteractionResponse {
    }

    record ContextualQuestion(
            String interactionType,
            String dimensionCode,
            String questionId,
            String instruction,
            String prompt,
            List<Option> options
    ) implements TieBreakInteractionResponse {
    }

    record Option(
            String optionId,
            String text
    ) {
    }
}
