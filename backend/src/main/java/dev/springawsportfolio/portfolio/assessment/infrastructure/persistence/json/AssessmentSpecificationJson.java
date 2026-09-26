package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json;

import java.util.List;

public record AssessmentSpecificationJson(
        List<DimensionJson> dimensions,
        QuestionnaireJson questionnaire,
        ScoringPolicyJson scoringPolicy,
        AmbiguityPolicyJson ambiguityPolicy,
        ClarificationPolicyJson clarificationPolicy,
        FinalizationPolicyJson finalizationPolicy
) {

    public record DimensionJson(
            String code,
            Integer position,
            String poleA,
            String poleB
    ) {
    }

    public record QuestionnaireJson(
            List<AnswerOptionJson> answerScale,
            List<QuestionJson> questions
    ) {
    }

    public record AnswerOptionJson(
            Integer value,
            String label
    ) {
    }

    public record QuestionJson(
            String questionId,
            Integer position,
            String prompt,
            String dimension,
            String keyedPole
    ) {
    }

    public record ScoringPolicyJson(
            String type,
            String revision,
            Integer answerCenter,
            Integer minimumAnswer,
            Integer maximumAnswer,
            Integer itemsPerDimension,
            Integer maximumAbsoluteRawScore
    ) {
    }

    public record AmbiguityPolicyJson(
            String type,
            String revision,
            Integer inclusiveThreshold
    ) {
    }

    public record ClarificationPolicyJson(
            String type,
            String revision
    ) {
    }

    public record FinalizationPolicyJson(
            String type,
            String revision
    ) {
    }
}