package dev.springawsportfolio.portfolio.assessment.domain.service;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.tiebreak.DimensionTieBreak;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AssessmentFinalizationService {

    public FinalAssessmentResult finalizeWithoutClarification(
            List<DimensionDefinition> dimensions,
            InitialAssessmentResult initialResult
    ) {
        Objects.requireNonNull(
                initialResult,
                "initialResult must not be null"
        );

        if (initialResult.hasAmbiguousDimensions()) {
            throw new IllegalStateException(
                    "assessment with ambiguous dimensions "
                            + "cannot be finalized without clarification"
            );
        }

        return finalizeIfReady(
                dimensions,
                initialResult,
                List.of(),
                List.of()
        )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "non-ambiguous assessment must be ready "
                                                + "for immediate finalization"
                                )
                );
    }

    public Optional<FinalAssessmentResult> finalizeIfReady(
            List<DimensionDefinition> dimensions,
            InitialAssessmentResult initialResult,
            List<DimensionClarification> clarifications,
            List<DimensionTieBreak> tieBreaks
    ) {
        Objects.requireNonNull(
                dimensions,
                "dimensions must not be null"
        );

        Objects.requireNonNull(
                initialResult,
                "initialResult must not be null"
        );

        Objects.requireNonNull(
                clarifications,
                "clarifications must not be null"
        );

        Objects.requireNonNull(
                tieBreaks,
                "tieBreaks must not be null"
        );

        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException(
                    "dimensions must not be empty"
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

        Map<DimensionCode, DimensionDefinition> definitionByDimension =
                indexDefinitions(
                        orderedDimensions
                );

        Map<DimensionCode, InitialDimensionResult> initialByDimension =
                indexInitialResults(
                        initialResult,
                        definitionByDimension
                );

        Map<DimensionCode, DimensionClarification> clarificationByDimension =
                indexClarifications(
                        clarifications,
                        initialByDimension
                );

        Map<DimensionCode, DimensionTieBreak> tieBreakByDimension =
                indexTieBreaks(
                        tieBreaks,
                        definitionByDimension,
                        initialByDimension,
                        clarificationByDimension
                );

        if (
                initialByDimension.size()
                        != orderedDimensions.size()
        ) {
            throw new IllegalArgumentException(
                    "initial result does not contain "
                            + "exactly one result per dimension"
            );
        }

        List<FinalDimensionConclusion> conclusions =
                new java.util.ArrayList<>();

        for (
                DimensionDefinition dimension
                : orderedDimensions
        ) {
            InitialDimensionResult initialDimension =
                    initialByDimension.get(
                            dimension.code()
                    );

            if (initialDimension == null) {
                throw new IllegalArgumentException(
                        "missing initial result for dimension: "
                                + dimension.code().value()
                );
            }

            if (!initialDimension.ambiguous()) {
                if (
                        clarificationByDimension.containsKey(
                                dimension.code()
                        )
                ) {
                    throw new IllegalStateException(
                            "non-ambiguous dimension must not have "
                                    + "a clarification: "
                                    + dimension.code().value()
                    );
                }

                if (
                        tieBreakByDimension.containsKey(
                                dimension.code()
                        )
                ) {
                    throw new IllegalStateException(
                            "non-ambiguous dimension must not have "
                                    + "a tie-break: "
                                    + dimension.code().value()
                    );
                }

                conclusions.add(
                        finalizeClearDimension(
                                dimension,
                                initialDimension
                        )
                );

                continue;
            }

            DimensionClarification clarification =
                    clarificationByDimension.get(
                            dimension.code()
                    );

            if (clarification == null) {
                throw new IllegalStateException(
                        "ambiguous dimension is missing clarification: "
                                + dimension.code().value()
                );
            }

            DimensionTieBreak tieBreak =
                    tieBreakByDimension.get(
                            dimension.code()
                    );

            if (!clarification.status().isTerminal()) {
                if (tieBreak != null) {
                    throw new IllegalStateException(
                            "tie-break must not exist before clarification "
                                    + "is terminal: "
                                    + dimension.code().value()
                    );
                }

                return Optional.empty();
            }

            Optional<FinalDimensionConclusion> conclusion =
                    finalizeAmbiguousDimensionIfReady(
                            dimension,
                            initialDimension,
                            clarification,
                            tieBreak
                    );

            if (conclusion.isEmpty()) {
                return Optional.empty();
            }

            conclusions.add(
                    conclusion.orElseThrow()
            );
        }

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

        return Optional.of(
                new FinalAssessmentResult(
                        finalType,
                        conclusions
                )
        );
    }

    private FinalDimensionConclusion finalizeClearDimension(
            DimensionDefinition dimension,
            InitialDimensionResult initialDimension
    ) {
        if (
                initialDimension.questionnairePreference()
                        == null
        ) {
            throw new IllegalStateException(
                    "non-ambiguous dimension must have "
                            + "a questionnaire preference"
            );
        }

        requireValidPole(
                dimension,
                initialDimension.questionnairePreference(),
                "questionnaire preference"
        );

        return new FinalDimensionConclusion(
                dimension.code(),
                initialDimension.questionnairePreference(),
                FinalDecisionSource.QUESTIONNAIRE
        );
    }

    private Optional<FinalDimensionConclusion>
    finalizeAmbiguousDimensionIfReady(
            DimensionDefinition dimension,
            InitialDimensionResult initialDimension,
            DimensionClarification clarification,
            DimensionTieBreak tieBreak
    ) {
        return switch (clarification.status()) {
            case CLARIFIED ->
                    finalizeClarifiedDimensionIfReady(
                            dimension,
                            initialDimension,
                            clarification,
                            tieBreak
                    );

            case SKIPPED ->
                    finalizeWithoutResolvedClarificationIfReady(
                            dimension,
                            initialDimension,
                            tieBreak
                    );

            case PENDING, IN_PROGRESS, FAILED_RETRYABLE ->
                    Optional.empty();
        };
    }

    private Optional<FinalDimensionConclusion>
    finalizeClarifiedDimensionIfReady(
            DimensionDefinition dimension,
            InitialDimensionResult initialDimension,
            DimensionClarification clarification,
            DimensionTieBreak tieBreak
    ) {
        if (clarification.result() == null) {
            throw new IllegalStateException(
                    "CLARIFIED clarification is missing result: "
                            + dimension.code().value()
            );
        }

        if (
                clarification.result().resolution()
                        == ClarificationResolution.UNCLEAR
        ) {
            return finalizeWithoutResolvedClarificationIfReady(
                    dimension,
                    initialDimension,
                    tieBreak
            );
        }

        if (tieBreak != null) {
            throw new IllegalStateException(
                    "resolved clarification must not also have tie-break: "
                            + dimension.code().value()
            );
        }

        PoleCode suggestedPole =
                Objects.requireNonNull(
                        clarification
                                .result()
                                .suggestedPole(),
                        "RESOLVED clarification must have suggestedPole"
                );

        requireValidPole(
                dimension,
                suggestedPole,
                "clarification suggested pole"
        );

        FinalDecisionSource decisionSource;

        if (initialDimension.questionnairePreference() == null) {
            decisionSource =
                    FinalDecisionSource.AI_CLARIFICATION;
        } else if (
                initialDimension
                        .questionnairePreference()
                        .equals(
                                suggestedPole
                        )
        ) {
            decisionSource =
                    FinalDecisionSource
                            .QUESTIONNAIRE_CONFIRMED_BY_CLARIFICATION;
        } else {
            decisionSource =
                    FinalDecisionSource.AI_CLARIFICATION;
        }

        return Optional.of(
                new FinalDimensionConclusion(
                        dimension.code(),
                        suggestedPole,
                        decisionSource
                )
        );
    }

    private Optional<FinalDimensionConclusion>
    finalizeWithoutResolvedClarificationIfReady(
            DimensionDefinition dimension,
            InitialDimensionResult initialDimension,
            DimensionTieBreak tieBreak
    ) {
        if (!initialDimension.exactTie()) {
            if (tieBreak != null) {
                throw new IllegalStateException(
                        "non-tie questionnaire baseline must not have "
                                + "a tie-break: "
                                + dimension.code().value()
                );
            }

            PoleCode questionnairePreference =
                    Objects.requireNonNull(
                            initialDimension
                                    .questionnairePreference(),
                            "non-zero questionnaire baseline must have preference"
                    );

            requireValidPole(
                    dimension,
                    questionnairePreference,
                    "questionnaire preference"
            );

            return Optional.of(
                    new FinalDimensionConclusion(
                            dimension.code(),
                            questionnairePreference,
                            FinalDecisionSource.QUESTIONNAIRE_FALLBACK
                    )
            );
        }

        if (tieBreak == null) {
            return Optional.empty();
        }

        requireValidPole(
                dimension,
                tieBreak.resolvedPole(),
                "tie-break resolved pole"
        );

        FinalDecisionSource decisionSource =
                tieBreak.isContextualQuestionSelection()
                        ? FinalDecisionSource.TIE_BREAK_QUESTION
                        : FinalDecisionSource.USER_TIE_BREAK;

        return Optional.of(
                new FinalDimensionConclusion(
                        dimension.code(),
                        tieBreak.resolvedPole(),
                        decisionSource
                )
        );
    }

    private Map<DimensionCode, DimensionDefinition> indexDefinitions(
            List<DimensionDefinition> dimensions
    ) {
        Map<DimensionCode, DimensionDefinition> result =
                new HashMap<>();

        for (
                DimensionDefinition dimension
                : dimensions
        ) {
            Objects.requireNonNull(
                    dimension,
                    "dimensions must not contain null"
            );

            if (
                    result.put(
                            dimension.code(),
                            dimension
                    ) != null
            ) {
                throw new IllegalArgumentException(
                        "duplicate dimension definition: "
                                + dimension.code().value()
                );
            }
        }

        return result;
    }

    private Map<DimensionCode, InitialDimensionResult>
    indexInitialResults(
            InitialAssessmentResult initialResult,
            Map<DimensionCode, DimensionDefinition> definitions
    ) {
        Map<DimensionCode, InitialDimensionResult> result =
                new HashMap<>();

        for (
                InitialDimensionResult dimension
                : initialResult.dimensions()
        ) {
            if (!definitions.containsKey(dimension.dimension())) {
                throw new IllegalArgumentException(
                        "initial result contains unknown dimension: "
                                + dimension.dimension().value()
                );
            }

            if (
                    result.put(
                            dimension.dimension(),
                            dimension
                    ) != null
            ) {
                throw new IllegalArgumentException(
                        "duplicate initial result for dimension: "
                                + dimension.dimension().value()
                );
            }
        }

        return result;
    }

    private Map<DimensionCode, DimensionClarification>
    indexClarifications(
            List<DimensionClarification> clarifications,
            Map<DimensionCode, InitialDimensionResult> initialResults
    ) {
        Map<DimensionCode, DimensionClarification> result =
                new HashMap<>();

        AssessmentSessionId expectedSessionId =
                null;

        for (
                DimensionClarification clarification
                : clarifications
        ) {
            Objects.requireNonNull(
                    clarification,
                    "clarifications must not contain null"
            );

            if (expectedSessionId == null) {
                expectedSessionId =
                        clarification.sessionId();
            } else if (
                    !expectedSessionId.equals(
                            clarification.sessionId()
                    )
            ) {
                throw new IllegalStateException(
                        "clarifications from different sessions "
                                + "cannot be finalized together"
                );
            }

            InitialDimensionResult initial =
                    initialResults.get(
                            clarification.dimension()
                    );

            if (initial == null) {
                throw new IllegalStateException(
                        "clarification references unknown dimension: "
                                + clarification.dimension().value()
                );
            }

            if (!initial.ambiguous()) {
                throw new IllegalStateException(
                        "clarification exists for non-ambiguous dimension: "
                                + clarification.dimension().value()
                );
            }

            if (
                    result.put(
                            clarification.dimension(),
                            clarification
                    ) != null
            ) {
                throw new IllegalStateException(
                        "duplicate clarification for dimension: "
                                + clarification.dimension().value()
                );
            }
        }

        return result;
    }

    private Map<DimensionCode, DimensionTieBreak>
    indexTieBreaks(
            List<DimensionTieBreak> tieBreaks,
            Map<DimensionCode, DimensionDefinition> definitions,
            Map<DimensionCode, InitialDimensionResult> initialResults,
            Map<DimensionCode, DimensionClarification> clarifications
    ) {
        Map<DimensionCode, DimensionTieBreak> result =
                new HashMap<>();

        for (
                DimensionTieBreak tieBreak
                : tieBreaks
        ) {
            Objects.requireNonNull(
                    tieBreak,
                    "tieBreaks must not contain null"
            );

            DimensionDefinition definition =
                    definitions.get(
                            tieBreak.dimension()
                    );

            InitialDimensionResult initial =
                    initialResults.get(
                            tieBreak.dimension()
                    );

            if (
                    definition == null
                            || initial == null
            ) {
                throw new IllegalStateException(
                        "tie-break references unknown dimension: "
                                + tieBreak.dimension().value()
                );
            }

            if (
                    !initial.ambiguous()
                            || !initial.exactTie()
            ) {
                throw new IllegalStateException(
                        "tie-break is only valid for an ambiguous exact tie: "
                                + tieBreak.dimension().value()
                );
            }

            DimensionClarification clarification =
                    clarifications.get(
                            tieBreak.dimension()
                    );

            if (clarification == null) {
                throw new IllegalStateException(
                        "tie-break requires the dimension clarification lifecycle: "
                                + tieBreak.dimension().value()
                );
            }

            if (
                    !clarification.sessionId()
                            .equals(
                                    tieBreak.sessionId()
                            )
            ) {
                throw new IllegalStateException(
                        "tie-break and clarification belong to different sessions: "
                                + tieBreak.dimension().value()
                );
            }

            requireValidPole(
                    definition,
                    tieBreak.resolvedPole(),
                    "tie-break resolved pole"
            );

            if (
                    result.put(
                            tieBreak.dimension(),
                            tieBreak
                    ) != null
            ) {
                throw new IllegalStateException(
                        "duplicate tie-break for dimension: "
                                + tieBreak.dimension().value()
                );
            }
        }

        return result;
    }

    private void requireValidPole(
            DimensionDefinition dimension,
            PoleCode pole,
            String source
    ) {
        if (
                !dimension.poleA().equals(pole)
                        && !dimension.poleB().equals(pole)
        ) {
            throw new IllegalArgumentException(
                    source
                            + " is not valid for dimension "
                            + dimension.code().value()
                            + ": "
                            + pole.value()
            );
        }
    }
}
