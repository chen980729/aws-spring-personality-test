package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeContext;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationExecutionTicket;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationExecutionToken;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class PostgresClarificationRuntimeContextStoreIntegrationTest {

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
    AssessmentDefinitionRepository
            definitionRepository;

    @Autowired
    AssessmentDefinitionVersionRepository
            versionRepository;

    @Autowired
    AssessmentSessionRepository
            sessionRepository;

    @Autowired
    ClarificationRuntimeContextStore
            runtimeContextStore;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void roundTripsMessagesAndRefreshesWholeContextExpiry() {
        StartedClarification started =
                startClarification();

        Instant firstCreatedAt =
                Instant.parse(
                        "2026-10-07T10:00:00Z"
                );

        Instant firstExpiry =
                Instant.parse(
                        "2026-10-07T11:00:00Z"
                );

        assertTrue(
                runtimeContextStore
                        .appendIfLatestSequenceMatches(
                                started.session().id(),
                                started.dimension().code(),
                                started.ticket().executionToken(),
                                0,
                                new ClarificationRuntimeMessage(
                                        1,
                                        ClarificationRuntimeMessage.Role
                                                .ASSISTANT_QUESTION,
                                        "How do you usually recharge?",
                                        firstCreatedAt,
                                        firstExpiry
                                )
                        )
        );

        Instant refreshedExpiry =
                Instant.parse(
                        "2026-10-07T12:00:00Z"
                );

        assertTrue(
                runtimeContextStore
                        .appendIfLatestSequenceMatches(
                                started.session().id(),
                                started.dimension().code(),
                                started.ticket().executionToken(),
                                1,
                                new ClarificationRuntimeMessage(
                                        2,
                                        ClarificationRuntimeMessage.Role
                                                .USER_ANSWER,
                                        "Usually by spending time alone.",
                                        firstCreatedAt.plusSeconds(60),
                                        refreshedExpiry
                                )
                        )
        );

        ClarificationRuntimeContext loaded =
                runtimeContextStore.loadActive(
                        started.session().id(),
                        started.dimension().code(),
                        started.ticket().executionToken()
                );

        assertEquals(
                2,
                loaded.latestSequence()
        );

        assertEquals(
                ClarificationRuntimeMessage.Role.ASSISTANT_QUESTION,
                loaded.messages().getFirst().role()
        );

        assertEquals(
                ClarificationRuntimeMessage.Role.USER_ANSWER,
                loaded.messages().get(1).role()
        );

        assertEquals(
                refreshedExpiry,
                loaded.messages().getFirst().expiresAt()
        );

        assertEquals(
                refreshedExpiry,
                loaded.messages().get(1).expiresAt()
        );
    }

    @Test
    void rejectsDuplicateSequenceAndStaleExecutionToken() {
        StartedClarification started =
                startClarification();

        Instant createdAt =
                Instant.parse(
                        "2026-10-07T10:00:00Z"
                );

        Instant expiresAt =
                Instant.parse(
                        "2026-10-07T11:00:00Z"
                );

        ClarificationRuntimeMessage first =
                new ClarificationRuntimeMessage(
                        1,
                        ClarificationRuntimeMessage.Role.ASSISTANT_QUESTION,
                        "First question",
                        createdAt,
                        expiresAt
                );

        assertTrue(
                runtimeContextStore
                        .appendIfLatestSequenceMatches(
                                started.session().id(),
                                started.dimension().code(),
                                started.ticket().executionToken(),
                                0,
                                first
                        )
        );

        assertFalse(
                runtimeContextStore
                        .appendIfLatestSequenceMatches(
                                started.session().id(),
                                started.dimension().code(),
                                started.ticket().executionToken(),
                                0,
                                first
                        )
        );

        ClarificationExecutionToken staleToken =
                ClarificationExecutionToken.newToken();

        assertFalse(
                runtimeContextStore
                        .appendIfLatestSequenceMatches(
                                started.session().id(),
                                started.dimension().code(),
                                staleToken,
                                0,
                                first
                        )
        );

        assertTrue(
                runtimeContextStore
                        .loadActive(
                                started.session().id(),
                                started.dimension().code(),
                                staleToken
                        )
                        .isEmpty()
        );
    }

    @Test
    void deleteExecutionAndExpiryCleanupRemoveTemporaryContext() {
        StartedClarification first =
                startClarification();

        Instant createdAt =
                Instant.parse(
                        "2026-10-07T10:00:00Z"
                );

        Instant expiresAt =
                Instant.parse(
                        "2026-10-07T10:30:00Z"
                );

        assertTrue(
                runtimeContextStore
                        .appendIfLatestSequenceMatches(
                                first.session().id(),
                                first.dimension().code(),
                                first.ticket().executionToken(),
                                0,
                                new ClarificationRuntimeMessage(
                                        1,
                                        ClarificationRuntimeMessage.Role
                                                .ASSISTANT_QUESTION,
                                        "Temporary question",
                                        createdAt,
                                        expiresAt
                                )
                        )
        );

        runtimeContextStore.deleteExecution(
                first.session().id(),
                first.dimension().code(),
                first.ticket().executionToken()
        );

        assertTrue(
                runtimeContextStore
                        .loadActive(
                                first.session().id(),
                                first.dimension().code(),
                                first.ticket().executionToken()
                        )
                        .isEmpty()
        );

        StartedClarification second =
                startClarification();

        assertTrue(
                runtimeContextStore
                        .appendIfLatestSequenceMatches(
                                second.session().id(),
                                second.dimension().code(),
                                second.ticket().executionToken(),
                                0,
                                new ClarificationRuntimeMessage(
                                        1,
                                        ClarificationRuntimeMessage.Role
                                                .ASSISTANT_QUESTION,
                                        "Expired question",
                                        createdAt,
                                        expiresAt
                                )
                        )
        );

        assertEquals(
                0,
                runtimeContextStore.deleteExpired(
                        expiresAt.minusSeconds(1)
                )
        );

        assertEquals(
                1,
                runtimeContextStore.deleteExpired(
                        expiresAt
                )
        );

        assertTrue(
                runtimeContextStore
                        .loadActive(
                                second.session().id(),
                                second.dimension().code(),
                                second.ticket().executionToken()
                        )
                        .isEmpty()
        );
    }

    @Test
    void databaseRejectsRoleSequenceMismatch() {
        StartedClarification started =
                startClarification();

        UUID clarificationId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM assessment_dimension_clarifications
                        WHERE session_id = ?
                          AND dimension_code = ?
                        """,
                        UUID.class,
                        started.session().id().value(),
                        started.dimension().code().value()
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO assessment_clarification_runtime_messages (
                            clarification_id,
                            execution_token,
                            sequence_number,
                            message_role,
                            message_text,
                            created_at,
                            expires_at
                        )
                        VALUES (?, ?, 2, 'ASSISTANT_QUESTION', ?, ?, ?)
                        """,
                        clarificationId,
                        started.ticket().executionToken().value(),
                        "Invalid parity",
                        Timestamp.from(
                                Instant.parse(
                                        "2026-10-07T10:00:00Z"
                                )
                        ),
                        Timestamp.from(
                                Instant.parse(
                                        "2026-10-07T11:00:00Z"
                                )
                        )
                )
        );
    }

    private StartedClarification startClarification() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                "SIXTEEN_PERSONALITY"
                        )
                        .orElseThrow();

        AssessmentDefinitionVersion version =
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
                        .orElseThrow();

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

        submitQuestionnaireService.execute(
                new SubmitQuestionnaireCommand(
                        userId,
                        session.id(),
                        neutralAnswers(
                                version
                        )
                )
        );

        DimensionDefinition dimension =
                version
                        .specification()
                        .dimensions()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        DimensionDefinition::position
                                )
                        )
                        .findFirst()
                        .orElseThrow();

        ClarificationExecutionTicket ticket =
                startDimensionClarificationService.execute(
                        new StartDimensionClarificationCommand(
                                userId,
                                session.id(),
                                dimension.code()
                        )
                );

        return new StartedClarification(
                session,
                dimension,
                ticket
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
                                question.questionId().value(),
                                version
                                        .specification()
                                        .scoringPolicy()
                                        .answerCenter()
                        )
                )
                .toList();
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
                "clarification-runtime-test-"
                        + id
                        + "@example.com",
                "Clarification Runtime Test User",
                "test-password-hash",
                Timestamp.from(
                        Instant.now()
                )
        );

        return new UserId(
                id
        );
    }

    private record StartedClarification(
            AssessmentSession session,
            DimensionDefinition dimension,
            ClarificationExecutionTicket ticket
    ) {
    }
}
