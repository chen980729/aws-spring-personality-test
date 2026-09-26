package dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;

import java.util.Objects;

public record Answer(
        QuestionId questionId,
        int value
) {

    public Answer {
        Objects.requireNonNull(
                questionId,
                "questionId must not be null"
        );
    }
}