package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireResult;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class AssessmentSubmissionPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    SubmitQuestionnaireService
            submitQuestionnaireService;

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
    JdbcTemplate jdbcTemplate;

    @Test
    void ambiguousSubmissionPersistsInitialResultAndPendingClarifications() {
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

        /*
         * SIXTEEN_PERSONALITY v1 uses balanced keying.
         *
         * Answering 5 for every item produces equal positive
         * and negative contributions within each dimension,
         * therefore rawScore = 0 for every dimension.
         */
        List<SubmitQuestionnaireCommand.AnswerInput> answers =
                version
                        .specification()
                        .questionnaire()
                        .questions()
                        .stream()
                        .map(question ->
                                new SubmitQuestionnaireCommand
                                        .AnswerInput(
                                        question
                                                .questionId()
                                                .value(),
                                        5
                                )
                        )
                        .toList();

        SubmitQuestionnaireResult result =
                submitQuestionnaireService.execute(
                        new SubmitQuestionnaireCommand(
                                userId,
                                session.id(),
                                answers
                        )
                );

        assertEquals(
                AssessmentSessionStatus
                        .AWAITING_CLARIFICATION,
                result.status()
        );

        assertNull(
                result.finalResult()
        );

        assertEquals(
                version
                        .specification()
                        .dimensions()
                        .size(),
                result
                        .pendingClarificationDimensions()
                        .size()
        );

        Map<String, Object> row =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            status,
                            questionnaire_submitted_at,
                            initial_result,
                            final_result,
                            completed_at
                        FROM assessment_sessions
                        WHERE id = ?
                        """,
                        session.id().value()
                );

        assertEquals(
                "AWAITING_CLARIFICATION",
                row.get("status")
        );

        assertNotNull(
                row.get(
                        "questionnaire_submitted_at"
                )
        );

        assertNotNull(
                row.get(
                        "initial_result"
                )
        );

        assertNull(
                row.get(
                        "final_result"
                )
        );

        assertNull(
                row.get(
                        "completed_at"
                )
        );

        Integer clarificationCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM assessment_dimension_clarifications
                        WHERE session_id = ?
                          AND status = 'PENDING'
                        """,
                        Integer.class,
                        session.id().value()
                );

        assertEquals(
                version
                        .specification()
                        .dimensions()
                        .size(),
                clarificationCount
        );

        AssessmentSession reloaded =
                sessionRepository
                        .findOwnedById(
                                session.id(),
                                userId
                        )
                        .orElseThrow();

        assertNotNull(
                reloaded.initialResult()
        );

        assertNull(
                reloaded.finalResult()
        );
    }

    @Test
    void clearSubmissionPersistsFinalResultAndCompletesImmediately() {
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

        Map<String, DimensionDefinition>
                dimensionsByCode =
                version
                        .specification()
                        .dimensions()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        dimension ->
                                                dimension
                                                        .code()
                                                        .value(),
                                        dimension ->
                                                dimension
                                )
                        );

        List<SubmitQuestionnaireCommand.AnswerInput> answers =
                version
                        .specification()
                        .questionnaire()
                        .questions()
                        .stream()
                        .map(question ->
                                clearPoleAAnswer(
                                        question,
                                        dimensionsByCode
                                )
                        )
                        .toList();

        SubmitQuestionnaireResult result =
                submitQuestionnaireService.execute(
                        new SubmitQuestionnaireCommand(
                                userId,
                                session.id(),
                                answers
                        )
                );

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                result.status()
        );

        assertNotNull(
                result.initialResult()
        );

        assertNotNull(
                result.finalResult()
        );

        assertNotNull(
                result.completedAt()
        );

        assertTrue(
                result
                        .pendingClarificationDimensions()
                        .isEmpty()
        );

        Map<String, Object> row =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            status,
                            questionnaire_submitted_at,
                            initial_result,
                            final_result,
                            completed_at
                        FROM assessment_sessions
                        WHERE id = ?
                        """,
                        session.id().value()
                );

        assertEquals(
                "COMPLETED",
                row.get("status")
        );

        assertNotNull(
                row.get(
                        "questionnaire_submitted_at"
                )
        );

        assertNotNull(
                row.get(
                        "initial_result"
                )
        );

        assertNotNull(
                row.get(
                        "final_result"
                )
        );

        assertNotNull(
                row.get(
                        "completed_at"
                )
        );

        Integer clarificationCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM assessment_dimension_clarifications
                        WHERE session_id = ?
                        """,
                        Integer.class,
                        session.id().value()
                );

        assertEquals(
                0,
                clarificationCount
        );

        AssessmentSession reloaded =
                sessionRepository
                        .findOwnedById(
                                session.id(),
                                userId
                        )
                        .orElseThrow();

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                reloaded.status()
        );

        assertNotNull(
                reloaded.initialResult()
        );

        assertNotNull(
                reloaded.finalResult()
        );

        assertEquals(
                result
                        .finalResult()
                        .finalType(),
                reloaded
                        .finalResult()
                        .finalType()
        );
    }

    private SubmitQuestionnaireCommand.AnswerInput
    clearPoleAAnswer(
            QuestionDefinition question,
            Map<String, DimensionDefinition>
                    dimensionsByCode
    ) {
        DimensionDefinition dimension =
                dimensionsByCode.get(
                        question
                                .dimension()
                                .value()
                );

        int value =
                question
                        .keyedPole()
                        .equals(
                                dimension.poleA()
                        )
                        ? 5
                        : 1;

        return new SubmitQuestionnaireCommand
                .AnswerInput(
                question
                        .questionId()
                        .value(),
                value
        );
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
                "submission-test-"
                        + id
                        + "@example.com",
                "Submission Test User",
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
