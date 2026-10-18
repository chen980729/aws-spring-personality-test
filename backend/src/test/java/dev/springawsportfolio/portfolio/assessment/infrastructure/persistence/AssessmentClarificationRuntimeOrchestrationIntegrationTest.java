package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationAiCallCommitService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationAiCallPreparationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationExecutionRecoveryService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationRuntimeOrchestrator;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationRuntimeTurnResult;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ContinueDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.RetryDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.RetryDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationTurnConflictException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.query.clarification.CurrentClarificationTurnResult;
import dev.springawsportfolio.portfolio.assessment.application.query.clarification.GetCurrentClarificationTurnService;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiPort;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTechnicalException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTurn;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
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
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class AssessmentClarificationRuntimeOrchestrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    SubmitQuestionnaireService submitQuestionnaireService;

    @Autowired
    StartDimensionClarificationService startService;

    @Autowired
    RetryDimensionClarificationService retryService;

    @Autowired
    ClarificationAiCallPreparationService preparationService;

    @Autowired
    ClarificationAiCallCommitService commitService;

    @Autowired
    ClarificationExecutionRecoveryService recoveryService;

    @Autowired
    SkipDimensionClarificationService skipDimensionClarificationService;

    @Autowired
    RestartAssessmentSessionService restartAssessmentSessionService;

    @Autowired
    AssessmentDefinitionRepository definitionRepository;

    @Autowired
    AssessmentDefinitionVersionRepository versionRepository;

    @Autowired
    AssessmentSessionRepository sessionRepository;

    @Autowired
    DimensionClarificationRepository clarificationRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    GetCurrentClarificationTurnService currentTurnService;

    @Test
    void readOnlyCurrentTurnRecoversPersistedQuestionsAndNeverReinvokesProvider() {
        SubmittedClarification submitted = submittedClarification();

        QueueAiPort aiPort = new QueueAiPort(
                request -> new ClarificationAiTurn.FollowUpQuestion(
                        "How do you usually recharge?"
                ),
                request -> new ClarificationAiTurn.FollowUpQuestion(
                        "And when you have a free weekend?"
                )
        );
        ClarificationRuntimeOrchestrator orchestrator = orchestrator(aiPort);

        ClarificationRuntimeTurnResult started = orchestrator.start(
                new StartDimensionClarificationCommand(
                        submitted.userId(),
                        submitted.session().id(),
                        submitted.dimension().code()
                )
        );

        CurrentClarificationTurnResult recovered = currentTurnService.execute(
                submitted.userId(),
                submitted.session().id(),
                submitted.dimension().code()
        );
        assertEquals(started.assistantSequence(), recovered.assistantSequence());
        assertEquals(started.followUpQuestion(), recovered.question());
        assertEquals(1, aiPort.callCount());

        assertThrows(
                AssessmentSessionNotFoundException.class,
                () -> currentTurnService.execute(
                        createUser(),
                        submitted.session().id(),
                        submitted.dimension().code()
                )
        );

        ClarificationRuntimeTurnResult continued = orchestrator.continueClarification(
                new ContinueDimensionClarificationCommand(
                        submitted.userId(),
                        submitted.session().id(),
                        submitted.dimension().code(),
                        recovered.assistantSequence(),
                        "I usually spend some time alone."
                )
        );

        CurrentClarificationTurnResult afterContinue = currentTurnService.execute(
                submitted.userId(),
                submitted.session().id(),
                submitted.dimension().code()
        );
        assertEquals(continued.assistantSequence(), afterContinue.assistantSequence());
        assertEquals(3, afterContinue.assistantSequence());
        assertEquals("And when you have a free weekend?", afterContinue.question());
        assertEquals(2, aiPort.callCount());

        skipDimensionClarificationService.execute(
                new SkipDimensionClarificationCommand(
                        submitted.userId(),
                        submitted.session().id(),
                        submitted.dimension().code()
                )
        );

        assertThrows(
                ClarificationNotAllowedException.class,
                () -> currentTurnService.execute(
                        submitted.userId(),
                        submitted.session().id(),
                        submitted.dimension().code()
                )
        );
        assertEquals(2, aiPort.callCount());
    }

    @Test
    void startFollowUpContinueAndAcceptJudgement() {
        SubmittedClarification submitted =
                submittedClarification();

        QueueAiPort aiPort =
                new QueueAiPort(
                        request -> {
                            assertEquals(
                                    1,
                                    request.currentModelTurn()
                            );

                            assertTrue(
                                    request.conversation().isEmpty()
                            );

                            return new ClarificationAiTurn
                                    .FollowUpQuestion(
                                    "How do you usually recharge?"
                            );
                        },
                        request -> {
                            assertEquals(
                                    2,
                                    request.currentModelTurn()
                            );

                            assertEquals(
                                    2,
                                    request.conversation().size()
                            );

                            assertEquals(
                                    ClarificationAiRequest
                                            .ConversationRole
                                            .USER_ANSWER,
                                    request
                                            .conversation()
                                            .getLast()
                                            .role()
                            );

                            assertEquals(
                                    "Usually by spending time alone.",
                                    request
                                            .conversation()
                                            .getLast()
                                            .text()
                            );

                            return resolvedJudgement(
                                    request
                            );
                        }
                );

        ClarificationRuntimeOrchestrator orchestrator =
                orchestrator(
                        aiPort
                );

        ClarificationRuntimeTurnResult start =
                orchestrator.start(
                        new StartDimensionClarificationCommand(
                                submitted.userId(),
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                );

        assertEquals(
                ClarificationRuntimeTurnResult.Status.FOLLOW_UP_REQUIRED,
                start.status()
        );

        assertEquals(
                1,
                start.assistantSequence()
        );

        DimensionClarification active =
                clarificationRepository
                        .findBySessionIdAndDimension(
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                        .orElseThrow();

        UUID executionToken =
                active
                        .activeExecutionToken()
                        .value();

        ClarificationRuntimeTurnResult completed =
                orchestrator.continueClarification(
                        new ContinueDimensionClarificationCommand(
                                submitted.userId(),
                                submitted.session().id(),
                                submitted.dimension().code(),
                                start.assistantSequence(),
                                "Usually by spending time alone."
                        )
                );

        assertEquals(
                ClarificationRuntimeTurnResult.Status.JUDGEMENT_ACCEPTED,
                completed.status()
        );

        assertEquals(
                DimensionClarificationStatus.CLARIFIED,
                clarificationRepository
                        .findBySessionIdAndDimension(
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                        .orElseThrow()
                        .status()
        );

        assertEquals(
                0,
                runtimeRowCount(
                        executionToken
                )
        );

        assertEquals(
                2,
                aiPort.callCount()
        );
    }

    @Test
    void staleClientReplayCannotAnswerTheNextFollowUp() {
        SubmittedClarification submitted =
                submittedClarification();

        QueueAiPort aiPort =
                new QueueAiPort(
                        request ->
                                new ClarificationAiTurn.FollowUpQuestion(
                                        "First follow-up?"
                                ),
                        request ->
                                new ClarificationAiTurn.FollowUpQuestion(
                                        "Second follow-up?"
                                )
                );

        ClarificationRuntimeOrchestrator orchestrator =
                orchestrator(
                        aiPort
                );

        ClarificationRuntimeTurnResult first =
                orchestrator.start(
                        new StartDimensionClarificationCommand(
                                submitted.userId(),
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                );

        ContinueDimensionClarificationCommand firstAnswer =
                new ContinueDimensionClarificationCommand(
                        submitted.userId(),
                        submitted.session().id(),
                        submitted.dimension().code(),
                        first.assistantSequence(),
                        "My first answer."
                );

        ClarificationRuntimeTurnResult second =
                orchestrator.continueClarification(
                        firstAnswer
                );

        assertEquals(
                ClarificationRuntimeTurnResult.Status.FOLLOW_UP_REQUIRED,
                second.status()
        );

        assertEquals(
                3,
                second.assistantSequence()
        );

        assertThrows(
                ClarificationTurnConflictException.class,
                () ->
                        orchestrator.continueClarification(
                                firstAnswer
                        )
        );

        assertEquals(
                2,
                aiPort.callCount()
        );
    }

    @Test
    void retryDoesNotDiscardValidCurrentAssistantQuestion() {
        SubmittedClarification submitted =
                submittedClarification();

        ClarificationRuntimeOrchestrator orchestrator =
                orchestrator(
                        new QueueAiPort(
                                request ->
                                        new ClarificationAiTurn
                                                .FollowUpQuestion(
                                                "Current question?"
                                        )
                        )
                );

        ClarificationRuntimeTurnResult first =
                orchestrator.start(
                        new StartDimensionClarificationCommand(
                                submitted.userId(),
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                );

        assertEquals(
                ClarificationRuntimeTurnResult.Status.FOLLOW_UP_REQUIRED,
                first.status()
        );

        assertThrows(
                ClarificationTurnConflictException.class,
                () ->
                        orchestrator.retry(
                                new RetryDimensionClarificationCommand(
                                        submitted.userId(),
                                        submitted.session().id(),
                                        submitted.dimension().code()
                                )
                        )
        );

        assertEquals(
                1,
                runtimeRowsForSession(
                        submitted.session().id()
                )
        );
    }

    @Test
    void technicalProviderFailureBecomesFailedRetryableAndCleansContext() {
        SubmittedClarification submitted =
                submittedClarification();

        ClarificationAiPort failingPort =
                request -> {
                    throw new ClarificationAiTechnicalException(
                            "synthetic provider failure"
                    );
                };

        ClarificationRuntimeOrchestrator orchestrator =
                orchestrator(
                        failingPort
                );

        ClarificationRuntimeTurnResult result =
                orchestrator.start(
                        new StartDimensionClarificationCommand(
                                submitted.userId(),
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                );

        assertEquals(
                ClarificationRuntimeTurnResult.Status.FAILED_RETRYABLE,
                result.status()
        );

        DimensionClarification clarification =
                clarificationRepository
                        .findBySessionIdAndDimension(
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                        .orElseThrow();

        assertEquals(
                DimensionClarificationStatus.FAILED_RETRYABLE,
                clarification.status()
        );

        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM assessment_clarification_runtime_messages
                        WHERE clarification_id = ?
                        """,
                        Integer.class,
                        clarification.id().value()
                )
        );
    }

    @Test
    void expiredInFlightAnswerCanBeRecoveredByRetry() {
        SubmittedClarification submitted =
                submittedClarification();

        QueueAiPort aiPort =
                new QueueAiPort(
                        request ->
                                new ClarificationAiTurn.FollowUpQuestion(
                                        "First follow-up?"
                                ),
                        this::resolvedJudgement
                );

        ClarificationRuntimeOrchestrator orchestrator =
                orchestrator(
                        aiPort
                );

        ClarificationRuntimeTurnResult first =
                orchestrator.start(
                        new StartDimensionClarificationCommand(
                                submitted.userId(),
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                );

        preparationService.prepareContinue(
                new ContinueDimensionClarificationCommand(
                        submitted.userId(),
                        submitted.session().id(),
                        submitted.dimension().code(),
                        first.assistantSequence(),
                        "This answer was persisted before the process died."
                )
        );

        jdbcTemplate.update(
                """
                UPDATE assessment_clarification_runtime_messages
                SET created_at = ?
                WHERE clarification_id = (
                    SELECT id
                    FROM assessment_dimension_clarifications
                    WHERE session_id = ?
                      AND dimension_code = ?
                )
                  AND sequence_number = 2
                """,
                Timestamp.from(
                        Instant.now().minusSeconds(
                                120
                        )
                ),
                submitted.session().id().value(),
                submitted.dimension().code().value()
        );

        ClarificationRuntimeTurnResult retried =
                orchestrator.retry(
                        new RetryDimensionClarificationCommand(
                                submitted.userId(),
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                );

        assertEquals(
                ClarificationRuntimeTurnResult.Status.JUDGEMENT_ACCEPTED,
                retried.status()
        );

        assertEquals(
                DimensionClarificationStatus.CLARIFIED,
                clarificationRepository
                        .findBySessionIdAndDimension(
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                        .orElseThrow()
                        .status()
        );

        assertEquals(
                2,
                aiPort.callCount()
        );
    }

    @Test
    void unexpectedAiRuntimeFailureCompensatesStateAndRethrows() {
        SubmittedClarification submitted =
                submittedClarification();

        ClarificationAiPort explodingPort =
                request -> {
                    throw new IllegalStateException(
                            "synthetic adapter bug"
                    );
                };

        ClarificationRuntimeOrchestrator orchestrator =
                orchestrator(
                        explodingPort
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        orchestrator.start(
                                new StartDimensionClarificationCommand(
                                        submitted.userId(),
                                        submitted.session().id(),
                                        submitted.dimension().code()
                                )
                        )
        );

        assertEquals(
                DimensionClarificationStatus.FAILED_RETRYABLE,
                clarificationRepository
                        .findBySessionIdAndDimension(
                                submitted.session().id(),
                                submitted.dimension().code()
                        )
                        .orElseThrow()
                        .status()
        );

        assertEquals(
                0,
                runtimeRowsForSession(
                        submitted.session().id()
                )
        );
    }

    @Test
    void skipDeletesTemporaryRuntimeContext() {
        SubmittedClarification submitted =
                submittedClarification();

        ClarificationRuntimeOrchestrator orchestrator =
                orchestrator(
                        new QueueAiPort(
                                request ->
                                        new ClarificationAiTurn
                                                .FollowUpQuestion(
                                                "Temporary follow-up?"
                                        )
                        )
                );

        orchestrator.start(
                new StartDimensionClarificationCommand(
                        submitted.userId(),
                        submitted.session().id(),
                        submitted.dimension().code()
                )
        );

        assertEquals(
                1,
                runtimeRowsForSession(
                        submitted.session().id()
                )
        );

        skipDimensionClarificationService.execute(
                new SkipDimensionClarificationCommand(
                        submitted.userId(),
                        submitted.session().id(),
                        submitted.dimension().code()
                )
        );

        assertEquals(
                0,
                runtimeRowsForSession(
                        submitted.session().id()
                )
        );
    }

    @Test
    void restartDeletesTemporaryRuntimeContext() {
        SubmittedClarification submitted =
                submittedClarification();

        ClarificationRuntimeOrchestrator orchestrator =
                orchestrator(
                        new QueueAiPort(
                                request ->
                                        new ClarificationAiTurn
                                                .FollowUpQuestion(
                                                "Temporary follow-up?"
                                        )
                        )
                );

        orchestrator.start(
                new StartDimensionClarificationCommand(
                        submitted.userId(),
                        submitted.session().id(),
                        submitted.dimension().code()
                )
        );

        assertEquals(
                1,
                runtimeRowsForSession(
                        submitted.session().id()
                )
        );

        restartAssessmentSessionService.execute(
                new RestartAssessmentSessionCommand(
                        submitted.userId(),
                        submitted.session().id()
                )
        );

        assertEquals(
                0,
                runtimeRowsForSession(
                        submitted.session().id()
                )
        );
    }

    private ClarificationRuntimeOrchestrator orchestrator(
            ClarificationAiPort aiPort
    ) {
        return new ClarificationRuntimeOrchestrator(
                startService,
                retryService,
                preparationService,
                commitService,
                recoveryService,
                aiPort
        );
    }

    private ClarificationAiTurn.Judgement resolvedJudgement(
            ClarificationAiRequest request
    ) {
        return new ClarificationAiTurn.Judgement(
                ClarificationResolution.RESOLVED,
                request
                        .targetDimension()
                        .poleA(),
                ClarificationConfidence.MEDIUM,
                "Synthetic integration-test judgement.",
                new ClarificationAiTurn.ModelIdentity(
                        "TEST_PROVIDER",
                        "test-model"
                )
        );
    }

    private SubmittedClarification submittedClarification() {
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

        return new SubmittedClarification(
                userId,
                session,
                dimension
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

    private int runtimeRowCount(
            UUID executionToken
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM assessment_clarification_runtime_messages
                WHERE execution_token = ?
                """,
                Integer.class,
                executionToken
        );
    }

    private int runtimeRowsForSession(
            AssessmentSessionId sessionId
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM assessment_clarification_runtime_messages message
                JOIN assessment_dimension_clarifications clarification
                  ON clarification.id = message.clarification_id
                WHERE clarification.session_id = ?
                """,
                Integer.class,
                sessionId.value()
        );
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
                "clarification-runtime-orchestration-"
                        + id
                        + "@example.com",
                "Clarification Runtime User",
                "test-password-hash",
                Timestamp.from(
                        Instant.now()
                )
        );

        return new UserId(
                id
        );
    }

    private record SubmittedClarification(
            UserId userId,
            AssessmentSession session,
            DimensionDefinition dimension
    ) {
    }

    private static final class QueueAiPort
            implements ClarificationAiPort {

        private final Deque<
                Function<
                        ClarificationAiRequest,
                        ClarificationAiTurn
                        >
                > responses =
                new ArrayDeque<>();

        private int callCount;

        @SafeVarargs
        QueueAiPort(
                Function<
                        ClarificationAiRequest,
                        ClarificationAiTurn
                        >... responses
        ) {
            this.responses.addAll(
                    List.of(
                            responses
                    )
            );
        }

        @Override
        public ClarificationAiTurn nextTurn(
                ClarificationAiRequest request
        ) {
            callCount++;

            Function<
                    ClarificationAiRequest,
                    ClarificationAiTurn
                    > response =
                    responses.pollFirst();

            if (response == null) {
                throw new AssertionError(
                        "unexpected AI call"
                );
            }

            return response.apply(
                    request
            );
        }

        int callCount() {
            return callCount;
        }
    }
}
