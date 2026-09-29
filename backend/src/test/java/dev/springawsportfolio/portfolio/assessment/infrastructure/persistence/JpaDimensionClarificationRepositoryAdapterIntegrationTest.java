package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

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
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class JpaDimensionClarificationRepositoryAdapterIntegrationTest {

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
    DimensionClarificationRepository
            clarificationRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void pendingAndInProgressRoundTrip() {
        Fixture fixture =
                ambiguousFixture();

        List<DimensionClarification> clarifications =
                clarificationRepository.findBySessionId(
                        fixture.session().id()
                );

        assertEquals(
                fixture
                        .version()
                        .specification()
                        .dimensions()
                        .size(),
                clarifications.size()
        );

        DimensionClarification clarification =
                clarifications.getFirst();

        assertEquals(
                DimensionClarificationStatus.PENDING,
                clarification.status()
        );

        assertNull(
                clarification.result()
        );

        assertNull(
                clarification.aiProvenance()
        );

        assertNull(
                clarification.startedAt()
        );

        Instant startedAt =
                clarification
                        .updatedAt()
                        .plusSeconds(10);

        var executionToken =
                clarification.start(
                        startedAt
                );

        clarificationRepository.update(
                clarification
        );

        DimensionClarification reloaded =
                clarificationRepository
                        .findBySessionIdAndDimension(
                                fixture.session().id(),
                                clarification.dimension()
                        )
                        .orElseThrow();

        assertEquals(
                clarification.id(),
                reloaded.id()
        );

        assertEquals(
                DimensionClarificationStatus.IN_PROGRESS,
                reloaded.status()
        );

        assertEquals(
                executionToken,
                reloaded.activeExecutionToken()
        );

        assertEquals(
                startedAt,
                reloaded.startedAt()
        );

        assertEquals(
                startedAt,
                reloaded.updatedAt()
        );

        assertNull(
                reloaded.result()
        );

        assertNull(
                reloaded.aiProvenance()
        );
    }

    @Test
    void retryableFailureAndRetryRoundTripPreserveOriginalStartedAt() {
        Fixture fixture =
                ambiguousFixture();

        DimensionClarification clarification =
                firstClarification(
                        fixture
                );

        Instant startedAt =
                clarification
                        .updatedAt()
                        .plusSeconds(10);

        Instant failedAt =
                startedAt.plusSeconds(10);

        Instant retriedAt =
                failedAt.plusSeconds(10);

        var firstExecutionToken =
                clarification.start(
                        startedAt
                );

        clarificationRepository.update(
                clarification
        );

        clarification.failRetryable(
                firstExecutionToken,
                failedAt
        );

        clarificationRepository.update(
                clarification
        );

        DimensionClarification failed =
                reload(
                        fixture,
                        clarification.dimension()
                );

        assertEquals(
                DimensionClarificationStatus.FAILED_RETRYABLE,
                failed.status()
        );

        assertEquals(
                startedAt,
                failed.startedAt()
        );

        assertEquals(
                failedAt,
                failed.updatedAt()
        );

        assertNull(
                failed.result()
        );

        assertNull(
                failed.aiProvenance()
        );

        assertNull(
                failed.activeExecutionToken()
        );

        var retryExecutionToken =
                failed.retry(
                        retriedAt
                );

        clarificationRepository.update(
                failed
        );

        DimensionClarification retried =
                reload(
                        fixture,
                        clarification.dimension()
                );

        assertEquals(
                DimensionClarificationStatus.IN_PROGRESS,
                retried.status()
        );

        assertNotEquals(
                firstExecutionToken,
                retryExecutionToken
        );

        assertEquals(
                retryExecutionToken,
                retried.activeExecutionToken()
        );

        assertEquals(
                startedAt,
                retried.startedAt()
        );

        assertEquals(
                retriedAt,
                retried.updatedAt()
        );
    }

    @Test
    void resolvedClarificationRoundTripPersistsResultAndProvenance() {
        Fixture fixture =
                ambiguousFixture();

        DimensionClarification clarification =
                firstClarification(
                        fixture
                );

        Instant startedAt =
                clarification
                        .updatedAt()
                        .plusSeconds(10);

        Instant acceptedAt =
                startedAt.plusSeconds(10);

        clarification.start(
                startedAt
        );

        clarificationRepository.update(
                clarification
        );

        ClarificationResult result =
                new ClarificationResult(
                        ClarificationResolution.RESOLVED,
                        new PoleCode("I"),
                        ClarificationConfidence.HIGH,
                        "Persisted resolved clarification summary."
                );

        AIProvenance provenance =
                new AIProvenance(
                        "test-provider",
                        "test-model",
                        "clarification-v1"
                );

        clarification.acceptResult(
                clarification.activeExecutionToken(),
                result,
                provenance,
                acceptedAt
        );

        clarificationRepository.update(
                clarification
        );

        DimensionClarification reloaded =
                reload(
                        fixture,
                        clarification.dimension()
                );

        assertEquals(
                DimensionClarificationStatus.CLARIFIED,
                reloaded.status()
        );

        assertNull(
                reloaded.activeExecutionToken()
        );

        assertEquals(
                result,
                reloaded.result()
        );

        assertEquals(
                provenance,
                reloaded.aiProvenance()
        );

        assertEquals(
                startedAt,
                reloaded.startedAt()
        );

        assertEquals(
                acceptedAt,
                reloaded.acceptedAt()
        );

        assertEquals(
                acceptedAt,
                reloaded.updatedAt()
        );
    }

    @Test
    void unclearClarificationRoundTripKeepsSuggestedPoleNull() {
        Fixture fixture =
                ambiguousFixture();

        DimensionClarification clarification =
                firstClarification(
                        fixture
                );

        Instant startedAt =
                clarification
                        .updatedAt()
                        .plusSeconds(10);

        Instant acceptedAt =
                startedAt.plusSeconds(10);

        clarification.start(
                startedAt
        );

        clarificationRepository.update(
                clarification
        );

        ClarificationResult result =
                new ClarificationResult(
                        ClarificationResolution.UNCLEAR,
                        null,
                        ClarificationConfidence.LOW,
                        "Persisted clarification remained unclear."
                );

        AIProvenance provenance =
                new AIProvenance(
                        "test-provider",
                        "test-model",
                        "clarification-v1"
                );

        clarification.acceptResult(
                clarification.activeExecutionToken(),
                result,
                provenance,
                acceptedAt
        );

        clarificationRepository.update(
                clarification
        );

        DimensionClarification reloaded =
                reload(
                        fixture,
                        clarification.dimension()
                );

        assertEquals(
                ClarificationResolution.UNCLEAR,
                reloaded
                        .result()
                        .resolution()
        );

        assertNull(
                reloaded
                        .result()
                        .suggestedPole()
        );

        assertEquals(
                ClarificationConfidence.LOW,
                reloaded
                        .result()
                        .confidence()
        );

        assertEquals(
                provenance,
                reloaded.aiProvenance()
        );
    }

    @Test
    void skippedAfterStartRoundTripPreservesStartedAtWithoutAcceptedData() {
        Fixture fixture =
                ambiguousFixture();

        DimensionClarification clarification =
                firstClarification(
                        fixture
                );

        Instant startedAt =
                clarification
                        .updatedAt()
                        .plusSeconds(10);

        Instant skippedAt =
                startedAt.plusSeconds(10);

        clarification.start(
                startedAt
        );

        clarificationRepository.update(
                clarification
        );

        clarification.skip(
                skippedAt
        );

        clarificationRepository.update(
                clarification
        );

        DimensionClarification reloaded =
                reload(
                        fixture,
                        clarification.dimension()
                );

        assertEquals(
                DimensionClarificationStatus.SKIPPED,
                reloaded.status()
        );

        assertEquals(
                startedAt,
                reloaded.startedAt()
        );

        assertEquals(
                skippedAt,
                reloaded.updatedAt()
        );

        assertNull(
                reloaded.result()
        );

        assertNull(
                reloaded.aiProvenance()
        );

        assertNull(
                reloaded.acceptedAt()
        );
    }

    @Test
    void databaseAllowsOnlyOneInProgressClarificationPerSession() {
        Fixture fixture =
                ambiguousFixture();

        List<DimensionClarification> clarifications =
                clarificationRepository.findBySessionId(
                        fixture.session().id()
                );

        assertTrue(
                clarifications.size() >= 2
        );

        DimensionClarification first =
                clarifications.get(0);

        DimensionClarification second =
                clarifications.get(1);

        first.start(
                first
                        .updatedAt()
                        .plusSeconds(10)
        );

        clarificationRepository.update(
                first
        );

        second.start(
                second
                        .updatedAt()
                        .plusSeconds(10)
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        clarificationRepository.update(
                                second
                        )
        );
    }

    private Fixture ambiguousFixture() {
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

        return new Fixture(
                session,
                version
        );
    }

    private DimensionClarification firstClarification(
            Fixture fixture
    ) {
        return clarificationRepository
                .findBySessionId(
                        fixture.session().id()
                )
                .getFirst();
    }

    private DimensionClarification reload(
            Fixture fixture,
            DimensionCode dimension
    ) {
        return clarificationRepository
                .findBySessionIdAndDimension(
                        fixture.session().id(),
                        dimension
                )
                .orElseThrow();
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
                "clarification-persistence-test-"
                        + id
                        + "@example.com",
                "Clarification Persistence Test User",
                "test-password-hash",
                Timestamp.from(
                        Instant.now()
                )
        );

        return new UserId(
                id
        );
    }

    private record Fixture(
            AssessmentSession session,
            AssessmentDefinitionVersion version
    ) {
    }
}
