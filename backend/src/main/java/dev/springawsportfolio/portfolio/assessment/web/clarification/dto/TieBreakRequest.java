package dev.springawsportfolio.portfolio.assessment.web.clarification.dto;

import jakarta.validation.constraints.AssertTrue;

public record TieBreakRequest(
        String selectedPole,
        String questionId,
        String selectedOptionId
) {

    @AssertTrue(
            message = "request must contain exactly one valid tie-break shape"
    )
    public boolean isValidShape() {
        return isLegacyShape()
                || isContextualShape();
    }

    public boolean isLegacyShape() {
        return hasText(selectedPole)
                && questionId == null
                && selectedOptionId == null;
    }

    public boolean isContextualShape() {
        return selectedPole == null
                && hasText(questionId)
                && hasText(selectedOptionId);
    }

    private static boolean hasText(
            String value
    ) {
        return value != null
                && !value.isBlank();
    }
}
