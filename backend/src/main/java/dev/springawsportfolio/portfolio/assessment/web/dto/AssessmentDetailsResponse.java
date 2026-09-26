package dev.springawsportfolio.portfolio.assessment.web.dto;

import java.util.List;
import java.util.Objects;

public record AssessmentDetailsResponse(
        String code,
        String name,
        String version,
        int questionCount,
        PublicQuestionnaireResponse questionnaire
) {

    public AssessmentDetailsResponse {
        Objects.requireNonNull(
                code,
                "code must not be null"
        );

        Objects.requireNonNull(
                name,
                "name must not be null"
        );

        Objects.requireNonNull(
                version,
                "version must not be null"
        );

        Objects.requireNonNull(
                questionnaire,
                "questionnaire must not be null"
        );
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
    }

    public record AnswerScaleOptionResponse(
            int value,
            String label
    ) {
    }
}