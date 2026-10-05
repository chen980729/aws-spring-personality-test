package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AssessmentSpecification;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssessmentSpecificationPersistenceMapperTest {

    private final JsonMapper jsonMapper =
            JsonMapper.builder().build();

    private final AssessmentSpecificationPersistenceMapper mapper =
            new AssessmentSpecificationPersistenceMapper(
                    jsonMapper
            );

    @Test
    void mapsJsonNodeToDomainSpecification() {
        JsonNode node =
                jsonMapper.readTree(
                        validJson(4)
                );

        AssessmentSpecification specification =
                mapper.toDomain(node);

        assertEquals(
                1,
                specification.dimensions().size()
        );

        assertEquals(
                "XY",
                specification
                        .dimensions()
                        .getFirst()
                        .code()
                        .value()
        );

        assertEquals(
                2,
                specification
                        .questionnaire()
                        .questions()
                        .size()
        );

        assertEquals(
                3,
                specification
                        .scoringPolicy()
                        .answerCenter()
        );

        assertEquals(
                4,
                specification
                        .scoringPolicy()
                        .maximumAbsoluteRawScore()
        );

        assertEquals(
                1,
                specification
                        .ambiguityPolicy()
                        .inclusiveThreshold()
        );
    }

    @Test
    void mapsContextualTieBreakQuestionsForV2() {
        JsonNode node =
                jsonMapper.readTree(
                        contextualJson()
                );

        AssessmentSpecification specification =
                mapper.toDomain(node);

        assertEquals(
                1,
                specification
                        .tieBreakQuestions()
                        .size()
        );

        assertEquals(
                "TB-XY-1",
                specification
                        .tieBreakQuestions()
                        .getFirst()
                        .questionId()
                        .value()
        );

        assertEquals(
                "Y",
                specification
                        .tieBreakQuestions()
                        .getFirst()
                        .options()
                        .get(1)
                        .resolvedPole()
                        .value()
        );

        assertEquals(
                "v2",
                specification
                        .finalizationPolicy()
                        .revision()
        );
    }

    @Test
    void keepsLegacyV1LoadableWhenTieBreakQuestionsFieldIsAbsent() {
        JsonNode node =
                jsonMapper.readTree(
                        validJson(4)
                );

        AssessmentSpecification specification =
                mapper.toDomain(node);

        assertEquals(
                0,
                specification
                        .tieBreakQuestions()
                        .size()
        );

        assertEquals(
                "v1",
                specification
                        .finalizationPolicy()
                        .revision()
        );
    }

    @Test
    void rejectsV2WhenTieBreakQuestionsFieldIsAbsent() {
        String json =
                validJson(4)
                        .replace(
                                "\"revision\": \"v1\"\n                  }\n                }",
                                "\"revision\": \"v2\"\n                  }\n                }"
                        );

        JsonNode node =
                jsonMapper.readTree(json);

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(node)
        );
    }

    @Test
    void rejectsPersistedMaximumRawScoreThatDoesNotMatchDerivedValue() {
        JsonNode node =
                jsonMapper.readTree(
                        validJson(99)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(node)
        );
    }

    @Test
    void rejectsUnsupportedScoringPolicyType() {
        String json =
                validJson(4)
                        .replace(
                                "CENTERED_BALANCED_KEYING",
                                "UNKNOWN_SCORING"
                        );

        JsonNode node =
                jsonMapper.readTree(json);

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(node)
        );
    }

    private String contextualJson() {
        return """
                {
                  "dimensions": [
                    {
                      "code": "XY",
                      "position": 1,
                      "poleA": "X",
                      "poleB": "Y"
                    }
                  ],
                  "tieBreakQuestions": [
                    {
                      "questionId": "TB-XY-1",
                      "dimension": "XY",
                      "instruction": "Choose the option that feels more natural.",
                      "prompt": "Which approach feels more natural?",
                      "options": [
                        {
                          "optionId": "TB-XY-01",
                          "text": "First option",
                          "resolvedPole": "X"
                        },
                        {
                          "optionId": "TB-XY-02",
                          "text": "Second option",
                          "resolvedPole": "Y"
                        }
                      ]
                    }
                  ],
                  "questionnaire": {
                    "answerScale": [
                      {"value": 1, "label": "Strongly Disagree"},
                      {"value": 2, "label": "Disagree"},
                      {"value": 3, "label": "Neutral"},
                      {"value": 4, "label": "Agree"},
                      {"value": 5, "label": "Strongly Agree"}
                    ],
                    "questions": [
                      {
                        "questionId": "Q1",
                        "position": 1,
                        "prompt": "Question 1",
                        "dimension": "XY",
                        "keyedPole": "X"
                      },
                      {
                        "questionId": "Q2",
                        "position": 2,
                        "prompt": "Question 2",
                        "dimension": "XY",
                        "keyedPole": "Y"
                      }
                    ]
                  },
                  "scoringPolicy": {
                    "type": "CENTERED_BALANCED_KEYING",
                    "revision": "v1",
                    "answerCenter": 3,
                    "minimumAnswer": 1,
                    "maximumAnswer": 5,
                    "itemsPerDimension": 2,
                    "maximumAbsoluteRawScore": 4
                  },
                  "ambiguityPolicy": {
                    "type": "ABSOLUTE_RAW_SCORE_THRESHOLD",
                    "revision": "v1",
                    "inclusiveThreshold": 1
                  },
                  "clarificationPolicy": {
                    "type": "DIMENSION_SCOPED_AI_CLARIFICATION",
                    "revision": "v1"
                  },
                  "finalizationPolicy": {
                    "type": "QUESTIONNAIRE_WITH_OPTIONAL_CLARIFICATION",
                    "revision": "v2"
                  }
                }
                """;
    }

    private String validJson(
            int maximumAbsoluteRawScore
    ) {
        return """
                {
                  "dimensions": [
                    {
                      "code": "XY",
                      "position": 1,
                      "poleA": "X",
                      "poleB": "Y"
                    }
                  ],
                  "questionnaire": {
                    "answerScale": [
                      {
                        "value": 1,
                        "label": "Strongly Disagree"
                      },
                      {
                        "value": 2,
                        "label": "Disagree"
                      },
                      {
                        "value": 3,
                        "label": "Neutral"
                      },
                      {
                        "value": 4,
                        "label": "Agree"
                      },
                      {
                        "value": 5,
                        "label": "Strongly Agree"
                      }
                    ],
                    "questions": [
                      {
                        "questionId": "Q1",
                        "position": 1,
                        "prompt": "Question 1",
                        "dimension": "XY",
                        "keyedPole": "X"
                      },
                      {
                        "questionId": "Q2",
                        "position": 2,
                        "prompt": "Question 2",
                        "dimension": "XY",
                        "keyedPole": "Y"
                      }
                    ]
                  },
                  "scoringPolicy": {
                    "type": "CENTERED_BALANCED_KEYING",
                    "revision": "v1",
                    "answerCenter": 3,
                    "minimumAnswer": 1,
                    "maximumAnswer": 5,
                    "itemsPerDimension": 2,
                    "maximumAbsoluteRawScore": %d
                  },
                  "ambiguityPolicy": {
                    "type": "ABSOLUTE_RAW_SCORE_THRESHOLD",
                    "revision": "v1",
                    "inclusiveThreshold": 1
                  },
                  "clarificationPolicy": {
                    "type": "DIMENSION_SCOPED_AI_CLARIFICATION",
                    "revision": "v1"
                  },
                  "finalizationPolicy": {
                    "type": "QUESTIONNAIRE_WITH_OPTIONAL_CLARIFICATION",
                    "revision": "v1"
                  }
                }
                """.formatted(
                maximumAbsoluteRawScore
        );
    }
}
