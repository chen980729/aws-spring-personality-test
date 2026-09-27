package dev.springawsportfolio.portfolio.assessment.domain.service;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssessmentFinalizationServiceTest {

    private AssessmentFinalizationService service;

    @BeforeEach
    void setUp() {
        service =
                new AssessmentFinalizationService();
    }

    @Test
    void finalizesClearDimensionsFromQuestionnaireOnly() {
        DimensionCode firstDimension =
                new DimensionCode("AB");

        DimensionCode secondDimension =
                new DimensionCode("CD");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        PoleCode poleC =
                new PoleCode("C");

        PoleCode poleD =
                new PoleCode("D");

        List<DimensionDefinition> dimensions =
                List.of(
                        new DimensionDefinition(
                                secondDimension,
                                2,
                                poleC,
                                poleD
                        ),
                        new DimensionDefinition(
                                firstDimension,
                                1,
                                poleA,
                                poleB
                        )
                );

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        secondDimension,
                                        -4,
                                        poleD,
                                        false
                                ),
                                new InitialDimensionResult(
                                        firstDimension,
                                        5,
                                        poleA,
                                        false
                                )
                        )
                );

        FinalAssessmentResult result =
                service.finalizeWithoutClarification(
                        dimensions,
                        initialResult
                );

        assertEquals(
                "AD",
                result.finalType()
        );

        assertEquals(
                2,
                result.dimensions().size()
        );

        assertEquals(
                firstDimension,
                result
                        .dimensions()
                        .get(0)
                        .dimension()
        );

        assertEquals(
                poleA,
                result
                        .dimensions()
                        .get(0)
                        .finalPreference()
        );

        assertEquals(
                FinalDecisionSource.QUESTIONNAIRE,
                result
                        .dimensions()
                        .get(0)
                        .decisionSource()
        );

        assertEquals(
                secondDimension,
                result
                        .dimensions()
                        .get(1)
                        .dimension()
        );

        assertEquals(
                poleD,
                result
                        .dimensions()
                        .get(1)
                        .finalPreference()
        );
    }

    @Test
    void rejectsImmediateFinalizationWhenAmbiguityExists() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        dimension,
                                        2,
                                        poleA,
                                        true
                                )
                        )
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.finalizeWithoutClarification(
                                List.of(
                                        new DimensionDefinition(
                                                dimension,
                                                1,
                                                poleA,
                                                poleB
                                        )
                                ),
                                initialResult
                        )
        );
    }

    @Test
    void rejectsMissingDimensionResult() {
        DimensionCode firstDimension =
                new DimensionCode("AB");

        DimensionCode secondDimension =
                new DimensionCode("CD");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        PoleCode poleC =
                new PoleCode("C");

        PoleCode poleD =
                new PoleCode("D");

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        firstDimension,
                                        4,
                                        poleA,
                                        false
                                )
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.finalizeWithoutClarification(
                                List.of(
                                        new DimensionDefinition(
                                                firstDimension,
                                                1,
                                                poleA,
                                                poleB
                                        ),
                                        new DimensionDefinition(
                                                secondDimension,
                                                2,
                                                poleC,
                                                poleD
                                        )
                                ),
                                initialResult
                        )
        );
    }
}
