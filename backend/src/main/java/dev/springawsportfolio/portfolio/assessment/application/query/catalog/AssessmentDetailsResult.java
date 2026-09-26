package dev.springawsportfolio.portfolio.assessment.application.query.catalog;

import java.util.List;
import java.util.Objects;

public record AssessmentDetailsResult(
        String code,
        String name,
        String versionCode,
        List<AnswerOptionResult> answerScale,
        List<QuestionResult> questions
) {

    public AssessmentDetailsResult {
        Objects.requireNonNull(
                code,
                "code must not be null"
        );

        Objects.requireNonNull(
                name,
                "name must not be null"
        );

        Objects.requireNonNull(
                versionCode,
                "versionCode must not be null"
        );

        Objects.requireNonNull(
                answerScale,
                "answerScale must not be null"
        );

        Objects.requireNonNull(
                questions,
                "questions must not be null"
        );

        answerScale = List.copyOf(answerScale);
        questions = List.copyOf(questions);
    }

    public int questionCount() {
        return questions.size();
    }

    public record AnswerOptionResult(
            int value,
            String label
    ) {
    }

    public record QuestionResult(
            String questionId,
            int position,
            String prompt
    ) {
    }
}
