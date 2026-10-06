package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakQuestionDefinition;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class JpaAssessmentDefinitionRepositoryAdapterIntegrationTest {

    private static final AssessmentDefinitionVersionId
            VERSION_1_0_ID =
            new AssessmentDefinitionVersionId(
                    UUID.fromString(
                            "9e2b641f-7600-4a4c-8f56-2d74c9b03e11"
                    )
            );

    private static final AssessmentDefinitionVersionId
            VERSION_1_1_ID =
            new AssessmentDefinitionVersionId(
                    UUID.fromString(
                            "b7a63f1e-60f4-4b5e-a0e3-1c1b1a110001"
                    )
            );

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    AssessmentDefinitionRepository definitionRepository;

    @Autowired
    AssessmentDefinitionVersionRepository versionRepository;

    @Test
    void loadsActivatedSixteenPersonalityVersionOnePointOne() {
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
                "1.1",
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
    void activatesVersionOnePointOneAndRetiresVersionOne() {
        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                "SIXTEEN_PERSONALITY"
                        )
                        .orElseThrow();

        AssessmentDefinitionVersion available =
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
                        .orElseThrow();

        assertEquals(
                "1.1",
                available.versionCode()
        );

        assertEquals(
                AssessmentDefinitionVersionStatus.AVAILABLE,
                available.status()
        );

        assertNotNull(
                available.publishedAt()
        );

        assertEquals(
                "v2",
                available
                        .specification()
                        .finalizationPolicy()
                        .revision()
        );

        assertEquals(
                4,
                available
                        .specification()
                        .tieBreakQuestions()
                        .size()
        );

        AssessmentDefinitionVersion retired =
                versionRepository
                        .findById(
                                VERSION_1_0_ID
                        )
                        .orElseThrow();

        assertEquals(
                "1.0",
                retired.versionCode()
        );

        assertEquals(
                AssessmentDefinitionVersionStatus.RETIRED,
                retired.status()
        );

        assertNotNull(
                retired.publishedAt()
        );

        assertEquals(
                "v1",
                retired
                        .specification()
                        .finalizationPolicy()
                        .revision()
        );

        assertTrue(
                retired
                        .specification()
                        .tieBreakQuestions()
                        .isEmpty()
        );
    }

    @Test
    void versionOnePointOneChangesOnlyFinalizationAndContextualTieBreakContent() {
        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                "SIXTEEN_PERSONALITY"
                        )
                        .orElseThrow();

        AssessmentDefinitionVersion versionOne =
                versionRepository
                        .findById(
                                VERSION_1_0_ID
                        )
                        .orElseThrow();

        AssessmentDefinitionVersion versionOnePointOne =
                versionRepository
                        .findById(
                                VERSION_1_1_ID
                        )
                        .orElseThrow();

        assertEquals(
                versionOne.specification().dimensions(),
                versionOnePointOne.specification().dimensions()
        );

        assertEquals(
                versionOne
                        .specification()
                        .questionnaire()
                        .answerScale(),
                versionOnePointOne
                        .specification()
                        .questionnaire()
                        .answerScale()
        );

        assertEquals(
                versionOne
                        .specification()
                        .questionnaire()
                        .questions(),
                versionOnePointOne
                        .specification()
                        .questionnaire()
                        .questions()
        );

        assertEquals(
                versionOne.specification().scoringPolicy(),
                versionOnePointOne.specification().scoringPolicy()
        );

        assertEquals(
                versionOne.specification().ambiguityPolicy(),
                versionOnePointOne.specification().ambiguityPolicy()
        );

        assertEquals(
                versionOne.specification().clarificationPolicy(),
                versionOnePointOne.specification().clarificationPolicy()
        );

        assertEquals(
                versionOne
                        .specification()
                        .finalizationPolicy()
                        .type(),
                versionOnePointOne
                        .specification()
                        .finalizationPolicy()
                        .type()
        );

        assertEquals(
                "v1",
                versionOne
                        .specification()
                        .finalizationPolicy()
                        .revision()
        );

        assertEquals(
                "v2",
                versionOnePointOne
                        .specification()
                        .finalizationPolicy()
                        .revision()
        );

        assertTrue(
                versionOne
                        .specification()
                        .tieBreakQuestions()
                        .isEmpty()
        );

        var questions =
                versionOnePointOne
                        .specification()
                        .tieBreakQuestions();

        assertTieBreakQuestion(
                questions.get(0),
                "TB-EI-1",
                "EI",
                "When you are trying to make sense of an important issue, which approach more often helps your thoughts become clear?",
                "TB-EI-01",
                "I start discussing it with someone and often discover what I think while talking.",
                "E",
                "TB-EI-02",
                "I first spend some time thinking it through privately, then share my thoughts once they have taken shape.",
                "I"
        );

        assertTieBreakQuestion(
                questions.get(1),
                "TB-SN-1",
                "SN",
                "When you face an unfamiliar problem with incomplete instructions, which starting point feels more natural?",
                "TB-SN-01",
                "I first gather concrete facts, examples, and what has worked before, then build the solution from there.",
                "S",
                "TB-SN-02",
                "I first form an overall picture of the patterns and possibilities, then use specific details to test or refine it.",
                "N"
        );

        assertTieBreakQuestion(
                questions.get(2),
                "TB-TF-1",
                "TF",
                "When two solutions are both reasonable but you must choose one, which consideration is more likely to guide your final decision?",
                "TB-TF-01",
                "I prefer the option supported by the most consistent criteria and defensible trade-offs, even if not everyone likes the outcome.",
                "T",
                "TB-TF-02",
                "I prefer the option that best accounts for the people involved and preserves trust, even if the rule is applied less uniformly.",
                "F"
        );

        assertTieBreakQuestion(
                questions.get(3),
                "TB-JP-1",
                "JP",
                "You begin an important project whose requirements may still change. Which approach feels more comfortable?",
                "TB-JP-01",
                "I establish a provisional plan, milestones, and next steps early, then revise them if new information appears.",
                "J",
                "TB-JP-02",
                "I keep the structure relatively open at first, gather more information, and commit to details when they become necessary.",
                "P"
        );
    }

    private void assertTieBreakQuestion(
            TieBreakQuestionDefinition question,
            String questionId,
            String dimension,
            String prompt,
            String firstOptionId,
            String firstOptionText,
            String firstPole,
            String secondOptionId,
            String secondOptionText,
            String secondPole
    ) {
        assertEquals(
                "Both options may describe you in different situations. "
                        + "If you had to choose, select the one that feels "
                        + "more natural to you most of the time.",
                question.instruction()
        );

        assertEquals(
                questionId,
                question.questionId().value()
        );

        assertEquals(
                dimension,
                question.dimension().value()
        );

        assertEquals(
                prompt,
                question.prompt()
        );

        assertEquals(
                firstOptionId,
                question.options().get(0).optionId().value()
        );

        assertEquals(
                firstOptionText,
                question.options().get(0).text()
        );

        assertEquals(
                firstPole,
                question.options().get(0).resolvedPole().value()
        );

        assertEquals(
                secondOptionId,
                question.options().get(1).optionId().value()
        );

        assertEquals(
                secondOptionText,
                question.options().get(1).text()
        );

        assertEquals(
                secondPole,
                question.options().get(1).resolvedPole().value()
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