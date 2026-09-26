package dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record QuestionnaireResponse(
        List<Answer> answers
) {

    public QuestionnaireResponse {
        Objects.requireNonNull(
                answers,
                "answers must not be null"
        );

        Set<QuestionId> questionIds =
                new HashSet<>();

        for (Answer answer : answers) {
            Objects.requireNonNull(
                    answer,
                    "answers must not contain null"
            );

            if (!questionIds.add(answer.questionId())) {
                throw new IllegalArgumentException(
                        "duplicate answer for question: "
                                + answer.questionId().value()
                );
            }
        }

        answers = List.copyOf(answers);
    }

    public static QuestionnaireResponse empty() {
        return new QuestionnaireResponse(
                List.of()
        );
    }
}
