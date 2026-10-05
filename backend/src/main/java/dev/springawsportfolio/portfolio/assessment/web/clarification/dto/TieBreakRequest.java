package dev.springawsportfolio.portfolio.assessment.web.clarification.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;

import java.util.HashMap;
import java.util.Map;

public final class TieBreakRequest {

    private String selectedPole;
    private String questionId;
    private String selectedOptionId;

    private final Map<String, Object> unknownFields =
            new HashMap<>();

    public TieBreakRequest() {
    }

    public String selectedPole() {
        return selectedPole;
    }

    public String questionId() {
        return questionId;
    }

    public String selectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedPole(
            String selectedPole
    ) {
        this.selectedPole = selectedPole;
    }

    public void setQuestionId(
            String questionId
    ) {
        this.questionId = questionId;
    }

    public void setSelectedOptionId(
            String selectedOptionId
    ) {
        this.selectedOptionId = selectedOptionId;
    }

    @JsonAnySetter
    public void captureUnknownField(
            String fieldName,
            Object value
    ) {
        unknownFields.put(
                fieldName,
                value
        );
    }

    @AssertTrue(
            message = "request must contain exactly one valid tie-break shape"
    )
    public boolean isValidShape() {
        return unknownFields.isEmpty()
                && (
                isLegacyShape()
                        || isContextualShape()
        );
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
