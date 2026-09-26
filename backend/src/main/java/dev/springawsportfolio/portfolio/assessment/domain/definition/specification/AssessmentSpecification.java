package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AssessmentSpecification {

    private final List<DimensionDefinition> dimensions;
    private final QuestionnaireDefinition questionnaire;
    private final ScoringPolicy scoringPolicy;
    private final AmbiguityPolicy ambiguityPolicy;
    private final ClarificationPolicy clarificationPolicy;
    private final FinalizationPolicy finalizationPolicy;

    public AssessmentSpecification(
            List<DimensionDefinition> dimensions,
            QuestionnaireDefinition questionnaire,
            ScoringPolicy scoringPolicy,
            AmbiguityPolicy ambiguityPolicy,
            ClarificationPolicy clarificationPolicy,
            FinalizationPolicy finalizationPolicy
    ) {
        Objects.requireNonNull(
                dimensions,
                "dimensions must not be null"
        );

        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException(
                    "dimensions must not be empty"
            );
        }

        for (DimensionDefinition dimension : dimensions) {
            Objects.requireNonNull(
                    dimension,
                    "dimensions must not contain null"
            );
        }

        this.dimensions = List.copyOf(dimensions);

        this.questionnaire = Objects.requireNonNull(
                questionnaire,
                "questionnaire must not be null"
        );

        this.scoringPolicy = Objects.requireNonNull(
                scoringPolicy,
                "scoringPolicy must not be null"
        );

        this.ambiguityPolicy = Objects.requireNonNull(
                ambiguityPolicy,
                "ambiguityPolicy must not be null"
        );

        this.clarificationPolicy = Objects.requireNonNull(
                clarificationPolicy,
                "clarificationPolicy must not be null"
        );

        this.finalizationPolicy = Objects.requireNonNull(
                finalizationPolicy,
                "finalizationPolicy must not be null"
        );

        validate();
    }

    public List<DimensionDefinition> dimensions() {
        return dimensions;
    }

    public QuestionnaireDefinition questionnaire() {
        return questionnaire;
    }

    public ScoringPolicy scoringPolicy() {
        return scoringPolicy;
    }

    public AmbiguityPolicy ambiguityPolicy() {
        return ambiguityPolicy;
    }

    public ClarificationPolicy clarificationPolicy() {
        return clarificationPolicy;
    }

    public FinalizationPolicy finalizationPolicy() {
        return finalizationPolicy;
    }

    private void validate() {
        Map<DimensionCode, DimensionDefinition> dimensionsByCode =
                validateDimensions();

        validateQuestionDimensions(dimensionsByCode);
        validateAnswerScale();
        validateQuestionDistribution(dimensionsByCode);
        validatePolicyConsistency();
    }

    private Map<DimensionCode, DimensionDefinition> validateDimensions() {
        Map<DimensionCode, DimensionDefinition> dimensionsByCode =
                new HashMap<>();

        Set<Integer> positions = new HashSet<>();

        for (DimensionDefinition dimension : dimensions) {

            if (
                    dimensionsByCode.put(
                            dimension.code(),
                            dimension
                    ) != null
            ) {
                throw new IllegalArgumentException(
                        "duplicate dimension code: "
                                + dimension.code().value()
                );
            }

            if (!positions.add(dimension.position())) {
                throw new IllegalArgumentException(
                        "duplicate dimension position: "
                                + dimension.position()
                );
            }
        }

        return dimensionsByCode;
    }

    private void validateQuestionDimensions(
            Map<DimensionCode, DimensionDefinition> dimensionsByCode
    ) {
        for (
                QuestionDefinition question
                : questionnaire.questions()
        ) {
            DimensionDefinition dimension =
                    dimensionsByCode.get(
                            question.dimension()
                    );

            if (dimension == null) {
                throw new IllegalArgumentException(
                        "question "
                                + question.questionId().value()
                                + " references unknown dimension "
                                + question.dimension().value()
                );
            }

            if (
                    !dimension.containsPole(
                            question.keyedPole()
                    )
            ) {
                throw new IllegalArgumentException(
                        "question "
                                + question.questionId().value()
                                + " uses pole "
                                + question.keyedPole().value()
                                + " outside dimension "
                                + dimension.code().value()
                );
            }
        }
    }

    private void validateAnswerScale() {
        Set<Integer> values = new HashSet<>();

        for (
                AnswerOption option
                : questionnaire.answerScale()
        ) {
            values.add(option.value());
        }

        for (
                int value = scoringPolicy.minimumAnswer();
                value <= scoringPolicy.maximumAnswer();
                value++
        ) {
            if (!values.contains(value)) {
                throw new IllegalArgumentException(
                        "answerScale does not contain value: "
                                + value
                );
            }
        }

        int expectedScaleSize =
                scoringPolicy.maximumAnswer()
                        - scoringPolicy.minimumAnswer()
                        + 1;

        if (values.size() != expectedScaleSize) {
            throw new IllegalArgumentException(
                    "answerScale contains values outside scoring range"
            );
        }
    }

    private void validateQuestionDistribution(
            Map<DimensionCode, DimensionDefinition> dimensionsByCode
    ) {
        Map<DimensionCode, Integer> totalCounts =
                new HashMap<>();

        Map<DimensionCode, Map<PoleCode, Integer>> poleCounts =
                new HashMap<>();

        for (
                QuestionDefinition question
                : questionnaire.questions()
        ) {
            totalCounts.merge(
                    question.dimension(),
                    1,
                    Integer::sum
            );

            poleCounts
                    .computeIfAbsent(
                            question.dimension(),
                            ignored -> new HashMap<>()
                    )
                    .merge(
                            question.keyedPole(),
                            1,
                            Integer::sum
                    );
        }

        for (
                DimensionDefinition dimension
                : dimensionsByCode.values()
        ) {
            int total = totalCounts.getOrDefault(
                    dimension.code(),
                    0
            );

            if (
                    total
                            != scoringPolicy.itemsPerDimension()
            ) {
                throw new IllegalArgumentException(
                        "dimension "
                                + dimension.code().value()
                                + " must contain "
                                + scoringPolicy.itemsPerDimension()
                                + " questions, but contains "
                                + total
                );
            }

            validateBalancedKeying(
                    dimension,
                    poleCounts.getOrDefault(
                            dimension.code(),
                            Map.of()
                    )
            );
        }
    }

    private void validateBalancedKeying(
            DimensionDefinition dimension,
            Map<PoleCode, Integer> poleCounts
    ) {
        if (
                scoringPolicy.type()
                        != ScoringPolicyType.CENTERED_BALANCED_KEYING
        ) {
            return;
        }

        if (
                scoringPolicy.itemsPerDimension() % 2 != 0
        ) {
            throw new IllegalArgumentException(
                    "balanced keying requires an even number "
                            + "of items per dimension"
            );
        }

        int expectedPerPole =
                scoringPolicy.itemsPerDimension() / 2;

        for (PoleCode pole : dimension.poles()) {
            int actual =
                    poleCounts.getOrDefault(
                            pole,
                            0
                    );

            if (actual != expectedPerPole) {
                throw new IllegalArgumentException(
                        "dimension "
                                + dimension.code().value()
                                + " must contain "
                                + expectedPerPole
                                + " questions keyed to pole "
                                + pole.value()
                                + ", but contains "
                                + actual
                );
            }
        }
    }

    private void validatePolicyConsistency() {
        int maximumAbsoluteRawScore =
                scoringPolicy.maximumAbsoluteRawScore();

        if (
                ambiguityPolicy.inclusiveThreshold()
                        > maximumAbsoluteRawScore
        ) {
            throw new IllegalArgumentException(
                    "ambiguity threshold must not exceed "
                            + "maximum absolute raw score: "
                            + maximumAbsoluteRawScore
            );
        }
    }
}