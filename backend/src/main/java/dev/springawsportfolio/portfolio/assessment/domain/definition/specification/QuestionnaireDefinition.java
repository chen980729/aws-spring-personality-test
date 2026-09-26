package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class QuestionnaireDefinition {

    private final List<AnswerOption> answerScale;
    private final List<QuestionDefinition> questions;

    public QuestionnaireDefinition(
            List<AnswerOption> answerScale,
            List<QuestionDefinition> questions
    ) {
        Objects.requireNonNull(
                answerScale,
                "answerScale must not be null"
        );
        Objects.requireNonNull(
                questions,
                "questions must not be null"
        );

        if (answerScale.isEmpty()) {
            throw new IllegalArgumentException(
                    "answerScale must not be empty"
            );
        }

        if (questions.isEmpty()) {
            throw new IllegalArgumentException(
                    "questions must not be empty"
            );
        }

        this.answerScale = List.copyOf(answerScale);
        this.questions = List.copyOf(questions);

        validateAnswerScale();
        validateQuestions();
    }

    public List<AnswerOption> answerScale() {
        return answerScale;
    }

    public List<QuestionDefinition> questions() {
        return questions;
    }

    private void validateAnswerScale() {
        Set<Integer> values = new HashSet<>();

        for (AnswerOption option : answerScale) {
            Objects.requireNonNull(
                    option,
                    "answerScale must not contain null"
            );

            if (!values.add(option.value())) {
                throw new IllegalArgumentException(
                        "answerScale contains duplicate value: "
                                + option.value()
                );
            }
        }
    }

    private void validateQuestions() {
        Set<QuestionId> ids = new HashSet<>();
        Set<Integer> positions = new HashSet<>();

        for (QuestionDefinition question : questions) {
            Objects.requireNonNull(
                    question,
                    "questions must not contain null"
            );

            if (!ids.add(question.questionId())) {
                throw new IllegalArgumentException(
                        "duplicate questionId: "
                                + question.questionId().value()
                );
            }

            if (!positions.add(question.position())) {
                throw new IllegalArgumentException(
                        "duplicate question position: "
                                + question.position()
                );
            }
        }
    }
}
