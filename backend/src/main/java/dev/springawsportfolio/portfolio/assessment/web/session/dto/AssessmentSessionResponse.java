package dev.springawsportfolio.portfolio.assessment.web.session.dto;

import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AssessmentSessionResponse(
        UUID id,
        BoundAssessmentResponse assessment,
        AssessmentSessionStatus status,
        QuestionnaireStateResponse questionnaire,
        Object initialResult,
        List<?> clarifications,
        List<?> tieBreaks,
        Object finalResult,
        AssessmentWorkflowResponse workflow,
        Instant createdAt,
        Instant completedAt,
        Instant abandonedAt
) {

    public AssessmentSessionResponse {
        Objects.requireNonNull(
                id,
                "id must not be null"
        );

        Objects.requireNonNull(
                assessment,
                "assessment must not be null"
        );

        Objects.requireNonNull(
                status,
                "status must not be null"
        );

        Objects.requireNonNull(
                questionnaire,
                "questionnaire must not be null"
        );

        Objects.requireNonNull(
                clarifications,
                "clarifications must not be null"
        );

        Objects.requireNonNull(
                tieBreaks,
                "tieBreaks must not be null"
        );

        Objects.requireNonNull(
                workflow,
                "workflow must not be null"
        );

        clarifications = List.copyOf(clarifications);
        tieBreaks = List.copyOf(tieBreaks);
    }

    public record BoundAssessmentResponse(
            String code,
            String version
    ) {
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
    }

    public record AssessmentWorkflowResponse(
            List<String> pendingClarificationDimensions,
            List<String> retryableClarificationDimensions,
            List<String> tieBreakRequiredDimensions,
            boolean completed
    ) {

        public AssessmentWorkflowResponse {
            pendingClarificationDimensions =
                    List.copyOf(
                            pendingClarificationDimensions
                    );

            retryableClarificationDimensions =
                    List.copyOf(
                            retryableClarificationDimensions
                    );

            tieBreakRequiredDimensions =
                    List.copyOf(
                            tieBreakRequiredDimensions
                    );
        }
    }
}
