package dev.springawsportfolio.portfolio.assessment.domain.service;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AmbiguityPolicy;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AmbiguityPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AnswerOption;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AssessmentSpecification;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ClarificationPolicy;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ClarificationPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.FinalizationPolicy;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.FinalizationPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionnaireDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ScoringPolicy;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ScoringPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionnaireScoringServiceTest {

    private static final DimensionCode DIMENSION =
            new DimensionCode("XY");

    private static final PoleCode POLE_X =
            new PoleCode("X");

    private static final PoleCode POLE_Y =
            new PoleCode("Y");

    private QuestionnaireScoringService service;

    private AssessmentSpecification specification;

    @BeforeEach
    void setUp() {
        service =
                new QuestionnaireScoringService();

        specification =
                specification();
    }

    @Test
    void scoresPoleAPreference() {
        QuestionnaireResponse response =
                response(
                        5,
                        2
                );

        InitialDimensionResult result =
                onlyDimension(
                        service.score(
                                specification,
                                response
                        )
                );

        /*
         * Q1 is keyed X:
         * 5 - 3 = +2
         *
         * Q2 is keyed Y:
         * -(2 - 3) = +1
         *
         * raw = +3
         */
        assertEquals(
                3,
                result.rawScore()
        );

        assertEquals(
                POLE_X,
                result.questionnairePreference()
        );

        assertFalse(
                result.ambiguous()
        );

        assertFalse(
                result.exactTie()
        );
    }

    @Test
    void scoresPoleBPreference() {
        QuestionnaireResponse response =
                response(
                        2,
                        5
                );

        InitialDimensionResult result =
                onlyDimension(
                        service.score(
                                specification,
                                response
                        )
                );

        assertEquals(
                -3,
                result.rawScore()
        );

        assertEquals(
                POLE_Y,
                result.questionnairePreference()
        );

        assertFalse(
                result.ambiguous()
        );
    }

    @Test
    void exactTieHasNoQuestionnairePreference() {
        QuestionnaireResponse response =
                response(
                        5,
                        5
                );

        InitialDimensionResult result =
                onlyDimension(
                        service.score(
                                specification,
                                response
                        )
                );

        assertEquals(
                0,
                result.rawScore()
        );

        assertNull(
                result.questionnairePreference()
        );

        assertTrue(
                result.exactTie()
        );

        assertTrue(
                result.ambiguous()
        );
    }

    @Test
    void treatsInclusiveThresholdAsAmbiguous() {
        QuestionnaireResponse response =
                response(
                        5,
                        3
                );

        InitialDimensionResult result =
                onlyDimension(
                        service.score(
                                specification,
                                response
                        )
                );

        assertEquals(
                2,
                result.rawScore()
        );

        assertEquals(
                POLE_X,
                result.questionnairePreference()
        );

        assertTrue(
                result.ambiguous()
        );
    }

    @Test
    void treatsScoreAboveThresholdAsClear() {
        QuestionnaireResponse response =
                response(
                        5,
                        2
                );

        InitialDimensionResult result =
                onlyDimension(
                        service.score(
                                specification,
                                response
                        )
                );

        assertEquals(
                3,
                result.rawScore()
        );

        assertFalse(
                result.ambiguous()
        );
    }

    @Test
    void scoringIsDeterministic() {
        QuestionnaireResponse response =
                response(
                        4,
                        2
                );

        InitialAssessmentResult first =
                service.score(
                        specification,
                        response
                );

        InitialAssessmentResult second =
                service.score(
                        specification,
                        response
                );

        assertEquals(
                first,
                second
        );
    }

    @Test
    void rejectsIncompleteQuestionnaireResponse() {
        QuestionnaireResponse response =
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q1"),
                                        5
                                )
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.score(
                                specification,
                                response
                        )
        );
    }

    @Test
    void rejectsUnknownQuestion() {
        QuestionnaireResponse response =
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q1"),
                                        5
                                ),
                                new Answer(
                                        new QuestionId("UNKNOWN"),
                                        3
                                )
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.score(
                                specification,
                                response
                        )
        );
    }

    @Test
    void rejectsAnswerOutsideDefinitionScale() {
        QuestionnaireResponse response =
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q1"),
                                        99
                                ),
                                new Answer(
                                        new QuestionId("Q2"),
                                        3
                                )
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.score(
                                specification,
                                response
                        )
        );
    }

    @Test
    void initialAssessmentResultReportsAmbiguousDimensions() {
        InitialAssessmentResult result =
                service.score(
                        specification,
                        response(
                                5,
                                3
                        )
                );

        assertTrue(
                result.hasAmbiguousDimensions()
        );

        assertEquals(
                List.of(DIMENSION),
                result.ambiguousDimensions()
        );
    }

    private InitialDimensionResult onlyDimension(
            InitialAssessmentResult result
    ) {
        assertEquals(
                1,
                result.dimensions().size()
        );

        return result
                .dimensions()
                .getFirst();
    }

    private QuestionnaireResponse response(
            int q1,
            int q2
    ) {
        return new QuestionnaireResponse(
                List.of(
                        new Answer(
                                new QuestionId("Q1"),
                                q1
                        ),
                        new Answer(
                                new QuestionId("Q2"),
                                q2
                        )
                )
        );
    }

    private AssessmentSpecification specification() {
        return new AssessmentSpecification(
                List.of(
                        new DimensionDefinition(
                                DIMENSION,
                                1,
                                POLE_X,
                                POLE_Y
                        )
                ),

                new QuestionnaireDefinition(
                        List.of(
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
                                        "Neutral"
                                ),
                                new AnswerOption(
                                        4,
                                        "Agree"
                                ),
                                new AnswerOption(
                                        5,
                                        "Strongly Agree"
                                )
                        ),

                        List.of(
                                new QuestionDefinition(
                                        new QuestionId("Q1"),
                                        1,
                                        "Question 1",
                                        DIMENSION,
                                        POLE_X
                                ),
                                new QuestionDefinition(
                                        new QuestionId("Q2"),
                                        2,
                                        "Question 2",
                                        DIMENSION,
                                        POLE_Y
                                )
                        )
                ),

                new ScoringPolicy(
                        ScoringPolicyType
                                .CENTERED_BALANCED_KEYING,
                        "v1",
                        3,
                        1,
                        5,
                        2
                ),

                new AmbiguityPolicy(
                        AmbiguityPolicyType
                                .ABSOLUTE_RAW_SCORE_THRESHOLD,
                        "v1",
                        2
                ),

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
}
