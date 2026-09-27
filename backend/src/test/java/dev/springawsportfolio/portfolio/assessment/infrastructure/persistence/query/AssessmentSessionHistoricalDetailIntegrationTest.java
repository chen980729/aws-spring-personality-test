package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.query;

import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class AssessmentSessionHistoricalDetailIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    SubmitQuestionnaireService
            submitQuestionnaireService;

    @Autowired
    GetAssessmentSessionService
            getAssessmentSessionService;

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
    void detailUsesPersistedClarificationAndTieBreakState() {
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

        submitQuestionnaireService.execute(
                new SubmitQuestionnaireCommand(
                        userId,
                        session.id(),
                        answers
                )
        );

        DimensionDefinition failedDimension =
                version
                        .specification()
                        .dimensions()
                        .getFirst();

        DimensionDefinition tieBreakDimension =
                version
                        .specification()
                        .dimensions()
                        .get(1);

        Instant startedAt =
                Instant.parse(
                        "2026-09-27T12:00:00Z"
                );

        Instant acceptedAt =
                Instant.parse(
                        "2026-09-27T12:01:00Z"
                );

        Instant decidedAt =
                Instant.parse(
                        "2026-09-27T12:02:00Z"
                );

        jdbcTemplate.update(
                """
                UPDATE assessment_dimension_clarifications
                SET status = 'FAILED_RETRYABLE',
                    started_at = ?,
                    updated_at = ?
                WHERE session_id = ?
                  AND dimension_code = ?
                """,
                Timestamp.from(startedAt),
                Timestamp.from(startedAt),
                session.id().value(),
                failedDimension.code().value()
        );

        jdbcTemplate.update(
                """
                UPDATE assessment_dimension_clarifications
                SET status = 'CLARIFIED',
                    result_outcome = 'UNCLEAR',
                    suggested_pole = NULL,
                    result_confidence = 'LOW',
                    result_summary = 'Persisted clarification was unclear.',
                    ai_provider = 'test-provider',
                    ai_model_identifier = 'test-model',
                    clarification_policy_revision = 'test-policy-v1',
                    started_at = ?,
                    accepted_at = ?,
                    updated_at = ?
                WHERE session_id = ?
                  AND dimension_code = ?
                """,
                Timestamp.from(startedAt),
                Timestamp.from(acceptedAt),
                Timestamp.from(acceptedAt),
                session.id().value(),
                tieBreakDimension.code().value()
        );

        jdbcTemplate.update(
                """
                INSERT INTO assessment_dimension_tie_breaks (
                    session_id,
                    dimension_code,
                    selected_pole,
                    decided_at
                )
                VALUES (?, ?, ?, ?)
                """,
                session.id().value(),
                tieBreakDimension.code().value(),
                tieBreakDimension.poleA().value(),
                Timestamp.from(decidedAt)
        );

        AssessmentSessionResult result =
                getAssessmentSessionService.execute(
                        userId,
                        session.id()
                );

        AssessmentSessionResult.ClarificationStateResult
                failedClarification =
                result
                        .clarifications()
                        .stream()
                        .filter(clarification ->
                                clarification
                                        .dimensionCode()
                                        .equals(
                                                failedDimension
                                                        .code()
                                                        .value()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                DimensionClarificationStatus.FAILED_RETRYABLE,
                failedClarification.status()
        );

        assertEquals(
                startedAt,
                failedClarification.startedAt()
        );

        assertTrue(
                result
                        .workflow()
                        .retryableClarificationDimensions()
                        .contains(
                                failedDimension
                                        .code()
                                        .value()
                        )
        );

        assertFalse(
                result
                        .workflow()
                        .pendingClarificationDimensions()
                        .contains(
                                failedDimension
                                        .code()
                                        .value()
                        )
        );

        AssessmentSessionResult.ClarificationStateResult
                clarified =
                result
                        .clarifications()
                        .stream()
                        .filter(clarification ->
                                clarification
                                        .dimensionCode()
                                        .equals(
                                                tieBreakDimension
                                                        .code()
                                                        .value()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                DimensionClarificationStatus.CLARIFIED,
                clarified.status()
        );

        assertNotNull(
                clarified.result()
        );

        assertEquals(
                "UNCLEAR",
                clarified
                        .result()
                        .resolution()
                        .name()
        );

        assertEquals(
                "LOW",
                clarified
                        .result()
                        .confidence()
        );

        assertEquals(
                "Persisted clarification was unclear.",
                clarified
                        .result()
                        .reasoningSummary()
        );

        assertEquals(
                acceptedAt,
                clarified.acceptedAt()
        );

        AssessmentSessionResult.TieBreakStateResult tieBreak =
                result
                        .tieBreaks()
                        .stream()
                        .filter(candidate ->
                                candidate
                                        .dimensionCode()
                                        .equals(
                                                tieBreakDimension
                                                        .code()
                                                        .value()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                tieBreakDimension.poleA().value(),
                tieBreak.selectedPole()
        );

        assertEquals(
                decidedAt,
                tieBreak.decidedAt()
        );

        assertFalse(
                result
                        .workflow()
                        .tieBreakRequiredDimensions()
                        .contains(
                                tieBreakDimension
                                        .code()
                                        .value()
                        )
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
                "historical-detail-test-"
                        + id
                        + "@example.com",
                "Historical Detail Test User",
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
