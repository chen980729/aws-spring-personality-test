package dev.springawsportfolio.portfolio.assessment.web.clarification.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClarificationMessageRequest(
        @NotNull Integer assistantSequence,
        @NotBlank String answer
) {
    @AssertTrue(message = "assistantSequence must be a positive odd number")
    public boolean isAssistantSequenceValid() {
        return assistantSequence == null
                || (assistantSequence > 0 && assistantSequence % 2 == 1);
    }

    @AssertTrue(message = "answer must be no more than 1000 Unicode code points")
    public boolean isAnswerLengthValid() {
        return answer == null
                || answer.codePointCount(0, answer.length()) <= 1000;
    }
}
