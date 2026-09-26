package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json;

import java.util.List;

public record QuestionnaireResponseJson(
        List<AnswerJson> answers
) {

    public record AnswerJson(
            String questionId,
            Integer value
    ) {
    }
}