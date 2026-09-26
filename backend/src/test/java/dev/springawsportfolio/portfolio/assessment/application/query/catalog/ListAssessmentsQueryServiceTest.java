package dev.springawsportfolio.portfolio.assessment.application.query.catalog;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.*;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListAssessmentsQueryServiceTest {

    private AssessmentDefinitionRepository definitionRepository;
    private AssessmentDefinitionVersionRepository versionRepository;

    private ListAssessmentsQueryService service;

    @BeforeEach
    void setUp() {
        definitionRepository =
                mock(AssessmentDefinitionRepository.class);

        versionRepository =
                mock(
                        AssessmentDefinitionVersionRepository.class
                );

        service =
                new ListAssessmentsQueryService(
                        definitionRepository,
                        versionRepository
                );
    }

    @Test
    void returnsOnlyDefinitionsWithAvailableVersion() {
        AssessmentDefinition available =
                definition(
                        "AVAILABLE_ASSESSMENT",
                        "Available Assessment"
                );

        AssessmentDefinition unavailable =
                definition(
                        "DRAFT_ONLY_ASSESSMENT",
                        "Draft Only Assessment"
                );

        when(definitionRepository.findAll())
                .thenReturn(
                        List.of(
                                unavailable,
                                available
                        )
                );

        when(
                versionRepository
                        .findAvailableByDefinitionId(
                                available.id()
                        )
        )
                .thenReturn(
                        Optional.of(
                                version(available.id())
                        )
                );

        when(
                versionRepository
                        .findAvailableByDefinitionId(
                                unavailable.id()
                        )
        )
                .thenReturn(
                        Optional.empty()
                );

        List<AssessmentCatalogItemResult> result =
                service.execute();

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "AVAILABLE_ASSESSMENT",
                result.getFirst().code()
        );

        assertEquals(
                "1.0",
                result.getFirst().availableVersion()
        );

        assertEquals(
                2,
                result.getFirst().questionCount()
        );
    }

    private AssessmentDefinition definition(
            String code,
            String name
    ) {
        return AssessmentDefinition.restore(
                new AssessmentDefinitionId(
                        UUID.randomUUID()
                ),
                code,
                name,
                Instant.parse(
                        "2026-01-01T00:00:00Z"
                )
        );
    }

    private AssessmentDefinitionVersion version(
            AssessmentDefinitionId definitionId
    ) {
        DimensionCode dimension =
                new DimensionCode("XY");

        PoleCode poleX = new PoleCode("X");
        PoleCode poleY = new PoleCode("Y");

        AssessmentSpecification specification =
                new AssessmentSpecification(
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
                                        new AnswerOption(1, "Low"),
                                        new AnswerOption(2, "Medium Low"),
                                        new AnswerOption(3, "Neutral"),
                                        new AnswerOption(4, "Medium High"),
                                        new AnswerOption(5, "High")
                                ),
                                List.of(
                                        new QuestionDefinition(
                                                new QuestionId("Q1"),
                                                1,
                                                "Question 1",
                                                dimension,
                                                poleX
                                        ),
                                        new QuestionDefinition(
                                                new QuestionId("Q2"),
                                                2,
                                                "Question 2",
                                                dimension,
                                                poleY
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

        return AssessmentDefinitionVersion.restore(
                new AssessmentDefinitionVersionId(
                        UUID.randomUUID()
                ),
                definitionId,
                "1.0",
                AssessmentDefinitionVersionStatus.AVAILABLE,
                specification,
                Instant.parse(
                        "2026-01-01T00:00:00Z"
                ),
                Instant.parse(
                        "2026-01-01T00:00:00Z"
                )
        );
    }
}
