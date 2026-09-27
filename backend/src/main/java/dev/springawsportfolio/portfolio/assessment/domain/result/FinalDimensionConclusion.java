package dev.springawsportfolio.portfolio.assessment.domain.result;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;

import java.util.Objects;

public record FinalDimensionConclusion(
        DimensionCode dimension,
        PoleCode finalPreference,
        FinalDecisionSource decisionSource
) {

    public FinalDimensionConclusion {
        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
        );

        Objects.requireNonNull(
                finalPreference,
                "finalPreference must not be null"
        );

        Objects.requireNonNull(
                decisionSource,
                "decisionSource must not be null"
        );
    }
}
