package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AssessmentResultPersistenceMapperTest {

    private final AssessmentResultPersistenceMapper mapper =
            new AssessmentResultPersistenceMapper(
                    JsonMapper.builder().build()
            );

    @Test
    void roundTripsInitialAssessmentResult() {
        InitialAssessmentResult original =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        new DimensionCode("EI"),
                                        -2,
                                        new PoleCode("I"),
                                        true
                                ),
                                new InitialDimensionResult(
                                        new DimensionCode("SN"),
                                        0,
                                        null,
                                        true
                                )
                        )
                );

        JsonNode json =
                mapper.toInitialJsonNode(
                        original
                );

        InitialAssessmentResult restored =
                mapper.toInitialDomain(
                        json
                );

        assertEquals(
                original,
                restored
        );

        assertNull(
                restored
                        .dimensions()
                        .get(1)
                        .questionnairePreference()
        );
    }

    @Test
    void roundTripsFinalAssessmentResult() {
        FinalAssessmentResult original =
                new FinalAssessmentResult(
                        "IN",
                        List.of(
                                new FinalDimensionConclusion(
                                        new DimensionCode("EI"),
                                        new PoleCode("I"),
                                        FinalDecisionSource.QUESTIONNAIRE
                                ),
                                new FinalDimensionConclusion(
                                        new DimensionCode("SN"),
                                        new PoleCode("N"),
                                        FinalDecisionSource.QUESTIONNAIRE
                                )
                        )
                );

        JsonNode json =
                mapper.toFinalJsonNode(
                        original
                );

        FinalAssessmentResult restored =
                mapper.toFinalDomain(
                        json
                );

        assertEquals(
                original,
                restored
        );
    }

    @Test
    void mapsMissingResultsToNull() {
        assertNull(
                mapper.toInitialDomain(
                        null
                )
        );

        assertNull(
                mapper.toFinalDomain(
                        null
                )
        );

        assertNull(
                mapper.toInitialJsonNode(
                        null
                )
        );

        assertNull(
                mapper.toFinalJsonNode(
                        null
                )
        );
    }
}
