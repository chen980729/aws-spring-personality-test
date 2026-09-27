package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json;

import java.util.List;

public final class AssessmentResultJson {

    private AssessmentResultJson() {
    }

    public record InitialResultJson(
            List<InitialDimensionJson> dimensions
    ) {
    }

    public record InitialDimensionJson(
            String dimension,
            int rawScore,
            String questionnairePreference,
            boolean ambiguous
    ) {
    }

    public record FinalResultJson(
            String finalType,
            List<FinalDimensionJson> dimensions
    ) {
    }

    public record FinalDimensionJson(
            String dimension,
            String finalPreference,
            String decisionSource
    ) {
    }
}
