package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipRemainingClarificationsCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipRemainingClarificationsService;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakService;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakNotRequiredException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionTieBreakRepository;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class AssessmentClarificationFinalizationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    SubmitQuestionnaireService
            submitQuestionnaireService;

    @Autowired
    SkipDimensionClarificationService
            skipDimensionClarificationService;

    @Autowired
    SkipRemainingClarificationsService
            skipRemainingClarificationsService;

    @Autowired
    SubmitDimensionTieBreakService
            submitDimensionTieBreakService;

    @Autowired
    AssessmentDefinitionRepository
            definitionRepository;

    @Autowired
    AssessmentDefinitionVersionRepository
            versionRepository;

    @Autowired
    AssessmentSessionRepository
            sessionRepository;

    @Autowired
    DimensionClarificationRepository
            clarificationRepository;

    @Autowired
    DimensionTieBreakRepository
            tieBreakRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void skippedExactTiesRequirePersistedUserTieBreaksBeforeCompletion() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                availableVersion(
                        definition
                );

        AssessmentSession session =
                createSession(
                        userId,
                        definition,
                        version
                );

        AssessmentSessionResult submitted =
                submitQuestionnaireService
                        .execute(
                                new SubmitQuestionnaireCommand(
                                        userId,
                                        session.id(),
                                        neutralAnswers(
                                                version
                                        )
                                )
                        )
                        .session();

        assertEquals(
                AssessmentSessionStatus.AWAITING_CLARIFICATION,
                submitted.status()
        );

        List<DimensionDefinition> orderedDimensions =
                version
                        .specification()
                        .dimensions()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        DimensionDefinition::position
                                )
                        )
                        .toList();

        assertEquals(
                orderedDimensions.size(),
                submitted
                        .workflow()
                        .pendingClarificationDimensions()
                        .size()
        );

        DimensionDefinition firstDimension =
                orderedDimensions.getFirst();

        AssessmentSessionResult afterOneSkip =
                skipDimensionClarificationService.execute(
                        new SkipDimensionClarificationCommand(
                                userId,
                                session.id(),
                                firstDimension.code()
                        )
                );

        assertEquals(
                AssessmentSessionStatus.AWAITING_CLARIFICATION,
                afterOneSkip.status()
        );

        assertTrue(
                afterOneSkip
                        .workflow()
                        .tieBreakRequiredDimensions()
                        .contains(
                                firstDimension
                                        .code()
                                        .value()
                        )
        );

        assertEquals(
                DimensionClarificationStatus.SKIPPED,
                clarificationRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                firstDimension.code()
                        )
                        .orElseThrow()
                        .status()
        );

        AssessmentSessionResult afterSkipRemaining =
                skipRemainingClarificationsService.execute(
                        new SkipRemainingClarificationsCommand(
                                userId,
                                session.id()
                        )
                );

        assertEquals(
                AssessmentSessionStatus.AWAITING_CLARIFICATION,
                afterSkipRemaining.status()
        );

        assertNull(
                afterSkipRemaining.finalResult()
        );

        assertTrue(
                afterSkipRemaining
                        .clarifications()
                        .stream()
                        .allMatch(clarification ->
                                clarification.status()
                                        == DimensionClarificationStatus.SKIPPED
                        )
        );

        assertEquals(
                orderedDimensions.size(),
                afterSkipRemaining
                        .workflow()
                        .tieBreakRequiredDimensions()
                        .size()
        );

        AssessmentSessionResult current =
                afterSkipRemaining;

        for (
                int index = 0;
                index < orderedDimensions.size();
                index++
        ) {
            DimensionDefinition dimension =
                    orderedDimensions.get(index);

            current =
                    submitDimensionTieBreakService.execute(
                            new SubmitDimensionTieBreakCommand(
                                    userId,
                                    session.id(),
                                    dimension.code(),
                                    dimension.poleA()
                            )
                    );

            if (index < orderedDimensions.size() - 1) {
                assertEquals(
                        AssessmentSessionStatus.AWAITING_CLARIFICATION,
                        current.status()
                );

                assertNull(
                        current.finalResult()
                );
            }
        }

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                current.status()
        );

        assertTrue(
                current.workflow().completed()
        );

        assertTrue(
                current
                        .workflow()
                        .tieBreakRequiredDimensions()
                        .isEmpty()
        );

        String expectedFinalType =
                orderedDimensions
                        .stream()
                        .map(dimension ->
                                dimension
                                        .poleA()
                                        .value()
                        )
                        .reduce(
                                "",
                                String::concat
                        );

        assertEquals(
                expectedFinalType,
                current
                        .finalResult()
                        .finalType()
        );

        assertTrue(
                current
                        .finalResult()
                        .dimensions()
                        .stream()
                        .allMatch(conclusion ->
                                conclusion.decisionSource()
                                        == FinalDecisionSource.USER_TIE_BREAK
                        )
        );

        assertEquals(
                orderedDimensions.size(),
                tieBreakRepository
                        .findBySessionId(
                                session.id()
                        )
                        .size()
        );

        DimensionDefinition lastDimension =
                orderedDimensions
                        .getLast();

        AssessmentSessionResult retry =
                submitDimensionTieBreakService.execute(
                        new SubmitDimensionTieBreakCommand(
                                userId,
                                session.id(),
                                lastDimension.code(),
                                lastDimension.poleA()
                        )
                );

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                retry.status()
        );

        assertEquals(
                orderedDimensions.size(),
                tieBreakRepository
                        .findBySessionId(
                                session.id()
                        )
                        .size()
        );

        assertThrows(
                TieBreakNotRequiredException.class,
                () ->
                        submitDimensionTieBreakService.execute(
                                new SubmitDimensionTieBreakCommand(
                                        userId,
                                        session.id(),
                                        lastDimension.code(),
                                        lastDimension.poleB()
                                )
                        )
        );

        AssessmentSession persisted =
                sessionRepository
                        .findOwnedById(
                                session.id(),
                                userId
                        )
                        .orElseThrow();

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                persisted.status()
        );

        assertEquals(
                expectedFinalType,
                persisted
                        .finalResult()
                        .finalType()
        );
    }

    private List<SubmitQuestionnaireCommand.AnswerInput> neutralAnswers(
            AssessmentDefinitionVersion version
    ) {
        return version
                .specification()
                .questionnaire()
                .questions()
                .stream()
                .map(question ->
                        new SubmitQuestionnaireCommand.AnswerInput(
                                question
                                        .questionId()
                                        .value(),
                                version
                                        .specification()
                                        .scoringPolicy()
                                        .answerCenter()
                        )
                )
                .toList();
    }

    private AssessmentSession createSession(
            UserId userId,
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version
    ) {
        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        userId,
                        definition.id(),
                        version.id(),
                        Instant.now()
                );

        assertTrue(
                sessionRepository.tryCreateActive(
                        session
                )
        );

        return session;
    }

    private AssessmentDefinition definition() {
        return definitionRepository
                .findByCode(
                        "SIXTEEN_PERSONALITY"
                )
                .orElseThrow();
    }

    private AssessmentDefinitionVersion availableVersion(
            AssessmentDefinition definition
    ) {
        return versionRepository
                .findAvailableByDefinitionId(
                        definition.id()
                )
                .orElseThrow();
    }

    private UserId createUser() {
        UUID id =
                UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO user_accounts (
                    id,
                    email,
                    display_name,
                    password_hash,
                    created_at,
                    version
                )
                VALUES (?, ?, ?, ?, ?, 0)
                """,
                id,
                "clarification-finalization-test-"
                        + id
                        + "@example.com",
                "Clarification Finalization Test User",
                "test-password-hash",
                Timestamp.from(
                        Instant.now()
                )
        );

        return new UserId(
                id
        );
    }
}
