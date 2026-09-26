package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class JpaAssessmentDefinitionRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    AssessmentDefinitionRepository definitionRepository;

    @Autowired
    AssessmentDefinitionVersionRepository versionRepository;

    @Test
    void loadsSeededSixteenPersonalityAvailableVersion() {
        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                "SIXTEEN_PERSONALITY"
                        )
                        .orElseThrow();

        assertEquals(
                "SIXTEEN_PERSONALITY",
                definition.code()
        );

        assertEquals(
                "Personality Type Explorer",
                definition.name()
        );

        AssessmentDefinitionVersion version =
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
                        .orElseThrow();

        assertEquals(
                definition.id(),
                version.definitionId()
        );

        assertEquals(
                "1.0",
                version.versionCode()
        );

        assertEquals(
                AssessmentDefinitionVersionStatus.AVAILABLE,
                version.status()
        );

        assertEquals(
                4,
                version
                        .specification()
                        .dimensions()
                        .size()
        );

        assertEquals(
                48,
                version
                        .specification()
                        .questionnaire()
                        .questions()
                        .size()
        );

        assertEquals(
                5,
                version
                        .specification()
                        .questionnaire()
                        .answerScale()
                        .size()
        );

        assertEquals(
                24,
                version
                        .specification()
                        .scoringPolicy()
                        .maximumAbsoluteRawScore()
        );

        assertEquals(
                2,
                version
                        .specification()
                        .ambiguityPolicy()
                        .inclusiveThreshold()
        );
    }

    @Test
    void returnsEmptyForUnknownAssessmentCode() {
        assertTrue(
                definitionRepository
                        .findByCode(
                                "NOT_A_REAL_ASSESSMENT"
                        )
                        .isEmpty()
        );
    }
}