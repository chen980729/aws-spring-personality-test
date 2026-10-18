package dev.springawsportfolio.portfolio.assessment.application.query.clarification;

import java.util.Objects;
import java.util.UUID;

public record CurrentClarificationTurnResult(
        UUID sessionId,
        String dimensionCode,
        int assistantSequence,
        String question
) {
    public CurrentClarificationTurnResult {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(dimensionCode, "dimensionCode must not be null");
        Objects.requireNonNull(question, "question must not be null");
        if (assistantSequence <= 0 || assistantSequence % 2 == 0) {
            throw new IllegalArgumentException("assistantSequence must be positive and odd");
        }
        if (question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
    }
}
