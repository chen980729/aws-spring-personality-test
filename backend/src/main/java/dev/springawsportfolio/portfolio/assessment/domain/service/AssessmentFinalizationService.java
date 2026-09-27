package dev.springawsportfolio.portfolio.assessment.domain.service;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AssessmentFinalizationService {

    public FinalAssessmentResult finalizeWithoutClarification(
            List<DimensionDefinition> dimensions,
            InitialAssessmentResult initialResult
    ) {
        Objects.requireNonNull(
                dimensions,
                "dimensions must not be null"
        );

        Objects.requireNonNull(
                initialResult,
                "initialResult must not be null"
        );

        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException(
                    "dimensions must not be empty"
            );
        }

        if (initialResult.hasAmbiguousDimensions()) {
            throw new IllegalStateException(
                    "assessment with ambiguous dimensions "
                            + "cannot be finalized without clarification"
            );
        }

        List<DimensionDefinition> orderedDimensions =
                dimensions
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        DimensionDefinition::position
                                )
                        )
                        .toList();

        if (
                orderedDimensions.size()
                        != initialResult
                        .dimensions()
                        .size()
        ) {
            throw new IllegalArgumentException(
                    "initial result does not contain "
                            + "exactly one result per dimension"
            );
        }

        List<FinalDimensionConclusion> conclusions =
                orderedDimensions
                        .stream()
                        .map(dimension ->
                                finalizeDimension(
                                        dimension,
                                        initialResult
                                )
                        )
                        .toList();

        String finalType =
                conclusions
                        .stream()
                        .map(conclusion ->
                                conclusion
                                        .finalPreference()
                                        .value()
                        )
                        .reduce(
                                "",
                                String::concat
                        );

        return new FinalAssessmentResult(
                finalType,
                conclusions
        );
    }

    private FinalDimensionConclusion finalizeDimension(
            DimensionDefinition dimension,
            InitialAssessmentResult initialResult
    ) {
        InitialDimensionResult initialDimension =
                initialResult
                        .dimensions()
                        .stream()
                        .filter(result ->
                                result
                                        .dimension()
                                        .equals(
                                                dimension.code()
                                        )
                        )
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "missing initial result "
                                                        + "for dimension: "
                                                        + dimension
                                                        .code()
                                                        .value()
                                        )
                        );

        if (initialDimension.ambiguous()) {
            throw new IllegalStateException(
                    "ambiguous dimension cannot be finalized "
                            + "directly from questionnaire: "
                            + dimension
                            .code()
                            .value()
            );
        }

        if (
                initialDimension
                        .questionnairePreference()
                        == null
        ) {
            throw new IllegalStateException(
                    "non-ambiguous dimension must have "
                            + "a questionnaire preference"
            );
        }

        return new FinalDimensionConclusion(
                dimension.code(),
                initialDimension.questionnairePreference(),
                FinalDecisionSource.QUESTIONNAIRE
        );
    }
}
