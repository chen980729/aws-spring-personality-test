package dev.springawsportfolio.portfolio.assessment.application.query.session;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GetSessionQuestionnaireResult(
        String assessmentCode,
        String assessmentVersion,
        List<QuestionResult> questions,
        List<AnswerOptionResult> answerScale,
        List<QuestionAnswerResult> answers,
        Instant submittedAt
) {

    public GetSessionQuestionnaireResult {
        Objects.requireNonNull(
                assessmentCode,
                "assessmentCode must not be null"
        );

        Objects.requireNonNull(
                assessmentVersion,
                "assessmentVersion must not be null"
        );

        Objects.requireNonNull(
                questions,
                "questions must not be null"
        );

        Objects.requireNonNull(
                answerScale,
                "answerScale must not be null"
        );

        Objects.requireNonNull(
                answers,
                "answers must not be null"
        );

        questions = List.copyOf(questions);
        answerScale = List.copyOf(answerScale);
        answers = List.copyOf(answers);
    }

    public boolean submitted() {
        return submittedAt != null;
    }

    public record QuestionResult(
            String questionId,
            int position,
            String prompt
    ) {

        public QuestionResult {
            Objects.requireNonNull(
                    questionId,
                    "questionId must not be null"
            );

            Objects.requireNonNull(
                    prompt,
                    "prompt must not be null"
            );
        }
    }

    public record AnswerOptionResult(
            int value,
            String label
    ) {

        public AnswerOptionResult {
            Objects.requireNonNull(
                    label,
                    "label must not be null"
            );
        }
    }

    public record QuestionAnswerResult(
            String questionId,
            int value
    ) {

        public QuestionAnswerResult {
            Objects.requireNonNull(
                    questionId,
                    "questionId must not be null"
            );
        }
    }
}
