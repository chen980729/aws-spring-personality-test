package dev.springawsportfolio.portfolio.assessment.web.session.dto;

import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssessmentSessionResponse(
        UUID id,
        BoundAssessmentResponse assessment,
        AssessmentSessionStatus status,
        QuestionnaireStateResponse questionnaire,
        InitialAssessmentResultResponse initialResult,
        List<ClarificationStateResponse> clarifications,
        List<TieBreakStateResponse> tieBreaks,
        FinalAssessmentResultResponse finalResult,
        AssessmentWorkflowResponse workflow,
        Instant createdAt,
        Instant completedAt,
        Instant abandonedAt
) {

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
    }

    public record QuestionAnswerResponse(
            String questionId,
            int value
    ) {
    }

    public record InitialAssessmentResultResponse(
            List<InitialDimensionResultResponse> dimensions
    ) {
    }

    public record InitialDimensionResultResponse(
            String dimensionCode,
            int rawScore,
            String questionnairePreference,
            boolean ambiguous,
            QuestionnaireEvidenceResponse evidence
    ) {
    }

    public record QuestionnaireEvidenceResponse(
            String poleA,
            double poleAPercentage,
            String poleB,
            double poleBPercentage
    ) {
    }

    public record ClarificationStateResponse(
            String dimensionCode,
            String status,
            ClarificationResultResponse result,
            Instant startedAt,
            Instant acceptedAt
    ) {
    }

    public sealed interface ClarificationResultResponse
            permits ResolvedClarificationResultResponse,
            UnclearClarificationResultResponse {
    }

    public record ResolvedClarificationResultResponse(
            String resolution,
            String suggestedPole,
            String confidence,
            String reasoningSummary
    ) implements ClarificationResultResponse {
    }

    public record UnclearClarificationResultResponse(
            String resolution,
            String suggestedPole,
            String confidence,
            String reasoningSummary
    ) implements ClarificationResultResponse {
    }

    public sealed interface TieBreakStateResponse
            permits LegacyTieBreakStateResponse,
            ContextualTieBreakStateResponse {

        String dimensionCode();

        Instant decidedAt();
    }

    public record LegacyTieBreakStateResponse(
            String dimensionCode,
            String selectedPole,
            Instant decidedAt
    ) implements TieBreakStateResponse {
    }

    public record ContextualTieBreakStateResponse(
            String dimensionCode,
            String questionId,
            String selectedOptionId,
            Instant decidedAt
    ) implements TieBreakStateResponse {
    }

    public record FinalAssessmentResultResponse(
            String finalType,
            List<FinalDimensionConclusionResponse> dimensions
    ) {
    }

    public record FinalDimensionConclusionResponse(
            String dimensionCode,
            String questionnairePreference,
            String finalPreference,
            String source,
            boolean overrodeBaseline
    ) {
    }

    public record AssessmentWorkflowResponse(
            List<String> pendingClarificationDimensions,
            List<String> retryableClarificationDimensions,
            List<String> tieBreakRequiredDimensions,
            boolean completed
    ) {
    }
}