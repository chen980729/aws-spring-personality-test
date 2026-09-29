package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationExecutionCompletionResult;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationExecutionCompletionStatus;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationExecutionTicket;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.CompleteClarificationExecutionService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.RetryDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.RetryDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.AIProvenance;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class AssessmentClarificationExecutionBoundaryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    SubmitQuestionnaireService
            submitQuestionnaireService;

    @Autowired
    StartDimensionClarificationService
            startDimensionClarificationService;

    @Autowired
    RetryDimensionClarificationService
            retryDimensionClarificationService;

    @Autowired
    CompleteClarificationExecutionService
            completeClarificationExecutionService;

    @Autowired
    RestartAssessmentSessionService
            restartAssessmentSessionService;

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
    JdbcTemplate jdbcTemplate;

    @Test
    void executionTokenRejectsStaleRetryAndLateResultAfterRestart() {
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

        submitQuestionnaireService.execute(
                new SubmitQuestionnaireCommand(
                        userId,
                        session.id(),
                        neutralAnswers(
                                version
                        )
                )
        );

        List<DimensionDefinition> dimensions =
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

        DimensionDefinition first =
                dimensions.getFirst();

        ClarificationExecutionTicket firstTicket =
                startDimensionClarificationService.execute(
                        new StartDimensionClarificationCommand(
                                userId,
                                session.id(),
                                first.code()
                        )
                );

        DimensionClarification started =
                clarificationRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                first.code()
                        )
                        .orElseThrow();

        assertEquals(
                DimensionClarificationStatus.IN_PROGRESS,
                started.status()
        );

        assertEquals(
                firstTicket.executionToken(),
                started.activeExecutionToken()
        );

        ClarificationExecutionCompletionResult failed =
                completeClarificationExecutionService.failRetryable(
                        firstTicket
                );

        assertTrue(
                failed.accepted()
        );

        assertEquals(
                AssessmentSessionStatus.AWAITING_CLARIFICATION,
                failed.session().status()
        );

        ClarificationExecutionTicket retryTicket =
                retryDimensionClarificationService.execute(
                        new RetryDimensionClarificationCommand(
                                userId,
                                session.id(),
                                first.code()
                        )
                );

        assertNotEquals(
                firstTicket.executionToken(),
                retryTicket.executionToken()
        );

        ClarificationExecutionCompletionResult staleFirstAttempt =
                completeClarificationExecutionService.accept(
                        firstTicket,
                        resolvedResult(
                                first
                        ),
                        provenance(
                                version
                        )
                );

        assertEquals(
                ClarificationExecutionCompletionStatus.STALE_DISCARDED,
                staleFirstAttempt.status()
        );

        DimensionClarification retried =
                clarificationRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                first.code()
                        )
                        .orElseThrow();

        assertEquals(
                DimensionClarificationStatus.IN_PROGRESS,
                retried.status()
        );

        assertEquals(
                retryTicket.executionToken(),
                retried.activeExecutionToken()
        );

        ClarificationExecutionCompletionResult acceptedRetry =
                completeClarificationExecutionService.accept(
                        retryTicket,
                        resolvedResult(
                                first
                        ),
                        provenance(
                                version
                        )
                );

        assertTrue(
                acceptedRetry.accepted()
        );

        assertEquals(
                DimensionClarificationStatus.CLARIFIED,
                clarificationRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                first.code()
                        )
                        .orElseThrow()
                        .status()
        );

        assertEquals(
                AssessmentSessionStatus.AWAITING_CLARIFICATION,
                acceptedRetry.session().status()
        );

        DimensionDefinition second =
                dimensions.get(1);

        ClarificationExecutionTicket secondTicket =
                startDimensionClarificationService.execute(
                        new StartDimensionClarificationCommand(
                                userId,
                                session.id(),
                                second.code()
                        )
                );

        restartAssessmentSessionService.execute(
                new RestartAssessmentSessionCommand(
                        userId,
                        session.id()
                )
        );

        ClarificationExecutionCompletionResult lateAfterRestart =
                completeClarificationExecutionService.accept(
                        secondTicket,
                        resolvedResult(
                                second
                        ),
                        provenance(
                                version
                        )
                );

        assertEquals(
                ClarificationExecutionCompletionStatus.STALE_DISCARDED,
                lateAfterRestart.status()
        );

        AssessmentSession abandoned =
                sessionRepository
                        .findOwnedById(
                                session.id(),
                                userId
                        )
                        .orElseThrow();

        assertEquals(
                AssessmentSessionStatus.ABANDONED,
                abandoned.status()
        );

        DimensionClarification abandonedInProgress =
                clarificationRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                second.code()
                        )
                        .orElseThrow();

        assertEquals(
                DimensionClarificationStatus.IN_PROGRESS,
                abandonedInProgress.status()
        );

        assertEquals(
                secondTicket.executionToken(),
                abandonedInProgress.activeExecutionToken()
        );

        assertNull(
                abandonedInProgress.result()
        );
    }

    private ClarificationResult resolvedResult(
            DimensionDefinition dimension
    ) {
        return new ClarificationResult(
                ClarificationResolution.RESOLVED,
                dimension.poleA(),
                ClarificationConfidence.HIGH,
                "Integration-test clarification result."
        );
    }

    private AIProvenance provenance(
            AssessmentDefinitionVersion version
    ) {
        return new AIProvenance(
                "test-provider",
                "test-model",
                version
                        .specification()
                        .clarificationPolicy()
                        .revision()
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
                "clarification-execution-test-"
                        + id
                        + "@example.com",
                "Clarification Execution Test User",
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
