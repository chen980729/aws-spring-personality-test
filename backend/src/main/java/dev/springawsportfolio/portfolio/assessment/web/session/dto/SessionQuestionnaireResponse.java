package dev.springawsportfolio.portfolio.assessment.web.session.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record SessionQuestionnaireResponse(
        BoundAssessmentResponse assessment,
        PublicQuestionnaireResponse questionnaire,
        QuestionnaireStateResponse response
) {

    public SessionQuestionnaireResponse {
        Objects.requireNonNull(
                assessment,
                "assessment must not be null"
        );

        Objects.requireNonNull(
                questionnaire,
                "questionnaire must not be null"
        );

        Objects.requireNonNull(
                response,
                "response must not be null"
        );
    }

    public record BoundAssessmentResponse(
            String code,
            String version
    ) {

        public BoundAssessmentResponse {
            Objects.requireNonNull(
                    code,
                    "code must not be null"
            );

            Objects.requireNonNull(
                    version,
                    "version must not be null"
            );
        }
    }

    public record PublicQuestionnaireResponse(
            List<PublicQuestionResponse> questions,
            List<AnswerScaleOptionResponse> answerScale
    ) {

        public PublicQuestionnaireResponse {
            Objects.requireNonNull(
                    questions,
                    "questions must not be null"
            );

            Objects.requireNonNull(
                    answerScale,
                    "answerScale must not be null"
            );

            questions = List.copyOf(questions);
            answerScale = List.copyOf(answerScale);
        }
    }

    public record PublicQuestionResponse(
            String questionId,
            int position,
            String prompt
    ) {

        public PublicQuestionResponse {
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

    public record AnswerScaleOptionResponse(
            int value,
            String label
    ) {

        public AnswerScaleOptionResponse {
            Objects.requireNonNull(
                    label,
                    "label must not be null"
            );
        }
    }

    public record QuestionnaireStateResponse(
            List<QuestionAnswerResponse> answers,
            boolean submitted,
            Instant submittedAt
    ) {

        public QuestionnaireStateResponse {
            Objects.requireNonNull(
                    answers,
                    "answers must not be null"
            );

            answers = List.copyOf(answers);
        }
    }

    public record QuestionAnswerResponse(
            String questionId,
            int value
    ) {

        public QuestionAnswerResponse {
            Objects.requireNonNull(
                    questionId,
                    "questionId must not be null"
            );
        }
    }
}
