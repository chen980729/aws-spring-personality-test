package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionnaireResponsePersistenceMapperTest {

    private final QuestionnaireResponsePersistenceMapper mapper =
            new QuestionnaireResponsePersistenceMapper(
                    JsonMapper.builder().build()
            );

    @Test
    void mapsEmptyDomainResponseToJsonAndBack() {
        QuestionnaireResponse original =
                QuestionnaireResponse.empty();

        JsonNode json =
                mapper.toJsonNode(
                        original
                );

        assertTrue(
                json.get("answers").isArray()
        );

        assertEquals(
                0,
                json.get("answers").size()
        );

        QuestionnaireResponse restored =
                mapper.toDomain(
                        json
                );

        assertTrue(
                restored.answers().isEmpty()
        );
    }

    @Test
    void roundTripsQuestionnaireResponse() {
        QuestionnaireResponse original =
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q1"),
                                        5
                                ),
                                new Answer(
                                        new QuestionId("Q2"),
                                        3
                                )
                        )
                );

        JsonNode json =
                mapper.toJsonNode(
                        original
                );

        assertEquals(
                "Q1",
                json
                        .get("answers")
                        .get(0)
                        .get("questionId")
                        .asText()
        );

        assertEquals(
                5,
                json
                        .get("answers")
                        .get(0)
                        .get("value")
                        .asInt()
        );

        QuestionnaireResponse restored =
                mapper.toDomain(
                        json
                );

        assertEquals(
                original,
                restored
        );
    }
}
