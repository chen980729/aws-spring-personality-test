package dev.springawsportfolio.portfolio.assessment.application.query.catalog;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionStatus;
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
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetAssessmentDetailsQueryServiceTest {

    private AssessmentDefinitionRepository definitionRepository;
    private AssessmentDefinitionVersionRepository versionRepository;

    private GetAssessmentDetailsQueryService service;

    @BeforeEach
    void setUp() {
        definitionRepository =
                mock(
                        AssessmentDefinitionRepository.class
                );

        versionRepository =
                mock(
                        AssessmentDefinitionVersionRepository.class
                );

        service =
                new GetAssessmentDetailsQueryService(
                        definitionRepository,
                        versionRepository
                );
    }

    @Test
    void returnsPublicAssessmentDetails() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(definition.id());

        when(
                definitionRepository.findByCode(
                        "TEST_ASSESSMENT"
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
        )
                .thenReturn(
                        Optional.of(version)
                );

        AssessmentDetailsResult result =
                service.execute(
                        "TEST_ASSESSMENT"
                );

        assertEquals(
                "TEST_ASSESSMENT",
                result.code()
        );

        assertEquals(
                "Test Assessment",
                result.name()
        );

        assertEquals(
                "1.0",
                result.versionCode()
        );

        assertEquals(
                5,
                result.answerScale().size()
        );

        assertEquals(
                2,
                result.questions().size()
        );

        assertEquals(
                "Q1",
                result
                        .questions()
                        .get(0)
                        .questionId()
        );

        assertEquals(
                "Question 1",
                result
                        .questions()
                        .get(0)
                        .prompt()
        );
    }

    @Test
    void returnsQuestionsOrderedByPosition() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(definition.id());

        when(
                definitionRepository.findByCode(
                        "TEST_ASSESSMENT"
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
        )
                .thenReturn(
                        Optional.of(version)
                );

        AssessmentDetailsResult result =
                service.execute(
                        "TEST_ASSESSMENT"
                );

        assertEquals(
                1,
                result.questions().get(0).position()
        );

        assertEquals(
                2,
                result.questions().get(1).position()
        );
    }

    @Test
    void throwsWhenAssessmentDoesNotExist() {
        when(
                definitionRepository.findByCode(
                        "UNKNOWN"
                )
        )
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                AssessmentNotFoundException.class,
                () -> service.execute("UNKNOWN")
        );
    }

    @Test
    void throwsWhenAssessmentHasNoAvailableVersion() {
        AssessmentDefinition definition =
                definition();

        when(
                definitionRepository.findByCode(
                        "TEST_ASSESSMENT"
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
        )
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                AssessmentNotFoundException.class,
                () ->
                        service.execute(
                                "TEST_ASSESSMENT"
                        )
        );
    }

    private AssessmentDefinition definition() {
        return AssessmentDefinition.restore(
                new AssessmentDefinitionId(
                        UUID.randomUUID()
                ),
                "TEST_ASSESSMENT",
                "Test Assessment",
                Instant.parse(
                        "2026-01-01T00:00:00Z"
                )
        );
    }

    private AssessmentDefinitionVersion version(
            AssessmentDefinitionId definitionId
    ) {
        return AssessmentDefinitionVersion.restore(
                new AssessmentDefinitionVersionId(
                        UUID.randomUUID()
                ),
                definitionId,
                "1.0",
                AssessmentDefinitionVersionStatus.AVAILABLE,
                specification(),
                Instant.parse(
                        "2026-01-01T00:00:00Z"
                ),
                Instant.parse(
                        "2026-01-01T00:00:00Z"
                )
        );
    }

    private AssessmentSpecification specification() {
        DimensionCode dimension =
                new DimensionCode("XY");

        PoleCode poleX =
                new PoleCode("X");

        PoleCode poleY =
                new PoleCode("Y");

        return new AssessmentSpecification(
                List.of(
                        new DimensionDefinition(
                                dimension,
                                1,
                                poleX,
                                poleY
                        )
                ),
                new QuestionnaireDefinition(
                        List.of(
                                new AnswerOption(
                                        5,
                                        "Strongly Agree"
                                ),
                                new AnswerOption(
                                        3,
                                        "Neutral"
                                ),
                                new AnswerOption(
                                        1,
                                        "Strongly Disagree"
                                ),
                                new AnswerOption(
                                        4,
                                        "Agree"
                                ),
                                new AnswerOption(
                                        2,
                                        "Disagree"
                                )
                        ),
                        List.of(
                                new QuestionDefinition(
                                        new QuestionId("Q2"),
                                        2,
                                        "Question 2",
                                        dimension,
                                        poleY
                                ),
                                new QuestionDefinition(
                                        new QuestionId("Q1"),
                                        1,
                                        "Question 1",
                                        dimension,
                                        poleX
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
                        1
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
