package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssessmentSpecificationTest {

    @Test
    void acceptsValidSpecification() {
        assertDoesNotThrow(
                () -> createSpecification(
                        validDimensions(),
                        validQuestions(),
                        validAnswerScale(),
                        validScoringPolicy(),
                        validAmbiguityPolicy()
                )
        );
    }

    @Test
    void rejectsDuplicateDimensionCode() {
        List<DimensionDefinition> dimensions =
                new ArrayList<>(
                        validDimensions()
                );

        dimensions.set(
                1,
                new DimensionDefinition(
                        new DimensionCode("EI"),
                        2,
                        new PoleCode("S"),
                        new PoleCode("N")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createSpecification(
                        dimensions,
                        validQuestions(),
                        validAnswerScale(),
                        validScoringPolicy(),
                        validAmbiguityPolicy()
                )
        );
    }

    @Test
    void rejectsQuestionReferencingUnknownDimension() {
        List<QuestionDefinition> questions =
                new ArrayList<>(
                        validQuestions()
                );

        QuestionDefinition original =
                questions.get(0);

        questions.set(
                0,
                new QuestionDefinition(
                        original.questionId(),
                        original.position(),
                        original.prompt(),
                        new DimensionCode("UNKNOWN"),
                        new PoleCode("X")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createSpecification(
                        validDimensions(),
                        questions,
                        validAnswerScale(),
                        validScoringPolicy(),
                        validAmbiguityPolicy()
                )
        );
    }

    @Test
    void rejectsPoleOutsideDimension() {
        List<QuestionDefinition> questions =
                new ArrayList<>(
                        validQuestions()
                );

        QuestionDefinition original =
                questions.get(0);

        questions.set(
                0,
                new QuestionDefinition(
                        original.questionId(),
                        original.position(),
                        original.prompt(),
                        new DimensionCode("EI"),
                        new PoleCode("T")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createSpecification(
                        validDimensions(),
                        questions,
                        validAnswerScale(),
                        validScoringPolicy(),
                        validAmbiguityPolicy()
                )
        );
    }

    @Test
    void rejectsWrongQuestionCountForDimension() {
        List<QuestionDefinition> questions =
                new ArrayList<>(
                        validQuestions()
                );

        questions.remove(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> createSpecification(
                        validDimensions(),
                        questions,
                        validAnswerScale(),
                        validScoringPolicy(),
                        validAmbiguityPolicy()
                )
        );
    }

    @Test
    void rejectsUnbalancedKeying() {
        List<QuestionDefinition> questions =
                new ArrayList<>(
                        validQuestions()
                );

        int index = findQuestionIndex(
                questions,
                "EI",
                "I"
        );

        QuestionDefinition original =
                questions.get(index);

        questions.set(
                index,
                new QuestionDefinition(
                        original.questionId(),
                        original.position(),
                        original.prompt(),
                        original.dimension(),
                        new PoleCode("E")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createSpecification(
                        validDimensions(),
                        questions,
                        validAnswerScale(),
                        validScoringPolicy(),
                        validAmbiguityPolicy()
                )
        );
    }

    @Test
    void rejectsAnswerScaleOutsideScoringRange() {
        List<AnswerOption> answerScale =
                new ArrayList<>(
                        validAnswerScale()
                );

        answerScale.add(
                new AnswerOption(
                        6,
                        "Extra"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createSpecification(
                        validDimensions(),
                        validQuestions(),
                        answerScale,
                        validScoringPolicy(),
                        validAmbiguityPolicy()
                )
        );
    }

    @Test
    void rejectsAmbiguityThresholdAboveMaximumPossibleScore() {
        AmbiguityPolicy invalidPolicy =
                new AmbiguityPolicy(
                        AmbiguityPolicyType
                                .ABSOLUTE_RAW_SCORE_THRESHOLD,
                        "v1",
                        25
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> createSpecification(
                        validDimensions(),
                        validQuestions(),
                        validAnswerScale(),
                        validScoringPolicy(),
                        invalidPolicy
                )
        );
    }

    @Test
    void rejectsNullDimensionElement() {
        List<DimensionDefinition> dimensions =
                new ArrayList<>(
                        validDimensions()
                );

        dimensions.set(
                0,
                null
        );

        assertThrows(
                NullPointerException.class,
                () -> createSpecification(
                        dimensions,
                        validQuestions(),
                        validAnswerScale(),
                        validScoringPolicy(),
                        validAmbiguityPolicy()
                )
        );
    }

    private AssessmentSpecification createSpecification(
            List<DimensionDefinition> dimensions,
            List<QuestionDefinition> questions,
            List<AnswerOption> answerScale,
            ScoringPolicy scoringPolicy,
            AmbiguityPolicy ambiguityPolicy
    ) {
        QuestionnaireDefinition questionnaire =
                new QuestionnaireDefinition(
                        answerScale,
                        questions
                );

        return new AssessmentSpecification(
                dimensions,
                questionnaire,
                scoringPolicy,
                ambiguityPolicy,
                new ClarificationPolicy(
                        ClarificationPolicyType
                                .DIMENSION_SCOPED_AI_CLARIFICATION,
                        "v1"
                ),
                new FinalizationPolicy(
                        FinalizationPolicyType
                                .QUESTIONNAIRE_WITH_OPTIONAL_CLARIFICATION,
                        "v1"
                )
        );
    }

    private List<DimensionDefinition> validDimensions() {
        return List.of(
                new DimensionDefinition(
                        new DimensionCode("EI"),
                        1,
                        new PoleCode("E"),
                        new PoleCode("I")
                ),
                new DimensionDefinition(
                        new DimensionCode("SN"),
                        2,
                        new PoleCode("S"),
                        new PoleCode("N")
                ),
                new DimensionDefinition(
                        new DimensionCode("TF"),
                        3,
                        new PoleCode("T"),
                        new PoleCode("F")
                ),
                new DimensionDefinition(
                        new DimensionCode("JP"),
                        4,
                        new PoleCode("J"),
                        new PoleCode("P")
                )
        );
    }

    private List<AnswerOption> validAnswerScale() {
        return List.of(
                new AnswerOption(
                        1,
                        "Strongly Disagree"
                ),
                new AnswerOption(
                        2,
                        "Disagree"
                ),
                new AnswerOption(
                        3,
                        "Neither Agree nor Disagree"
                ),
                new AnswerOption(
                        4,
                        "Agree"
                ),
                new AnswerOption(
                        5,
                        "Strongly Agree"
                )
        );
    }

    private ScoringPolicy validScoringPolicy() {
        return new ScoringPolicy(
                ScoringPolicyType
                        .CENTERED_BALANCED_KEYING,
                "v1",
                3,
                1,
                5,
                12
        );
    }

    private AmbiguityPolicy validAmbiguityPolicy() {
        return new AmbiguityPolicy(
                AmbiguityPolicyType
                        .ABSOLUTE_RAW_SCORE_THRESHOLD,
                "v1",
                2
        );
    }

    private List<QuestionDefinition> validQuestions() {
        List<QuestionDefinition> questions =
                new ArrayList<>();

        int position = 1;

        position = addDimensionQuestions(
                questions,
                position,
                "EI",
                "E",
                "I"
        );

        position = addDimensionQuestions(
                questions,
                position,
                "SN",
                "S",
                "N"
        );

        position = addDimensionQuestions(
                questions,
                position,
                "TF",
                "T",
                "F"
        );

        addDimensionQuestions(
                questions,
                position,
                "JP",
                "J",
                "P"
        );

        return questions;
    }

    private int addDimensionQuestions(
            List<QuestionDefinition> questions,
            int startPosition,
            String dimensionCode,
            String poleA,
            String poleB
    ) {
        int position = startPosition;

        for (int i = 0; i < 6; i++) {
            questions.add(
                    new QuestionDefinition(
                            new QuestionId(
                                    "Q" + position
                            ),
                            position,
                            "Question " + position,
                            new DimensionCode(
                                    dimensionCode
                            ),
                            new PoleCode(
                                    poleA
                            )
                    )
            );

            position++;
        }

        for (int i = 0; i < 6; i++) {
            questions.add(
                    new QuestionDefinition(
                            new QuestionId(
                                    "Q" + position
                            ),
                            position,
                            "Question " + position,
                            new DimensionCode(
                                    dimensionCode
                            ),
                            new PoleCode(
                                    poleB
                            )
                    )
            );

            position++;
        }

        return position;
    }

    private int findQuestionIndex(
            List<QuestionDefinition> questions,
            String dimensionCode,
            String poleCode
    ) {
        for (int i = 0; i < questions.size(); i++) {
            QuestionDefinition question =
                    questions.get(i);

            if (
                    question.dimension().value()
                            .equals(dimensionCode)
                            && question.keyedPole().value()
                            .equals(poleCode)
            ) {
                return i;
            }
        }

        throw new IllegalStateException(
                "test fixture question not found"
        );
    }
}
