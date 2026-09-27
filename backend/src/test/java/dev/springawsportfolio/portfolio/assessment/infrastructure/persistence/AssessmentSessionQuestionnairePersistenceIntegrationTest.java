package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class AssessmentSessionQuestionnairePersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    AssessmentSessionRepository sessionRepository;

    @Autowired
    AssessmentDefinitionRepository definitionRepository;

    @Autowired
    AssessmentDefinitionVersionRepository versionRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void persistsAndReloadsQuestionnaireSnapshotAndIncrementsVersion() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                availableVersion(
                        definition
                );

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

        Long versionBefore =
                databaseVersion(
                        session.id()
                );

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );

        transactionTemplate.executeWithoutResult(
                status -> {
                    AssessmentSession loaded =
                            sessionRepository
                                    .findOwnedById(
                                            session.id(),
                                            userId
                                    )
                                    .orElseThrow();

                    loaded.replaceQuestionnaireResponse(
                            new QuestionnaireResponse(
                                    List.of(
                                            new Answer(
                                                    new QuestionId("Q1"),
                                                    5
                                            ),
                                            new Answer(
                                                    new QuestionId("Q2"),
                                                    3
                                            )
                                    )
                            )
                    );

                    sessionRepository.update(
                            loaded
                    );
                }
        );

        AssessmentSession reloaded =
                sessionRepository
                        .findOwnedById(
                                session.id(),
                                userId
                        )
                        .orElseThrow();

        assertEquals(
                2,
                reloaded
                        .questionnaireResponse()
                        .answers()
                        .size()
        );

        assertEquals(
                "Q1",
                reloaded
                        .questionnaireResponse()
                        .answers()
                        .get(0)
                        .questionId()
                        .value()
        );

        assertEquals(
                5,
                reloaded
                        .questionnaireResponse()
                        .answers()
                        .get(0)
                        .value()
        );

        Long versionAfter =
                databaseVersion(
                        session.id()
                );

        assertEquals(
                versionBefore + 1,
                versionAfter
        );
    }

    @Test
    void concurrentDraftUpdatesAllowExactlyOneCommit()
            throws Exception {

        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                availableVersion(
                        definition
                );

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

        CountDownLatch loaded =
                new CountDownLatch(2);

        CountDownLatch startUpdate =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first =
                    executor.submit(
                            () ->
                                    concurrentUpdate(
                                            session.id(),
                                            userId,
                                            4,
                                            loaded,
                                            startUpdate
                                    )
                    );

            Future<Boolean> second =
                    executor.submit(
                            () ->
                                    concurrentUpdate(
                                            session.id(),
                                            userId,
                                            5,
                                            loaded,
                                            startUpdate
                                    )
                    );

            loaded.await();

            startUpdate.countDown();

            boolean firstSucceeded =
                    first.get();

            boolean secondSucceeded =
                    second.get();

            assertEquals(
                    1,
                    (firstSucceeded ? 1 : 0)
                            + (secondSucceeded ? 1 : 0)
            );

            AssessmentSession authoritative =
                    sessionRepository
                            .findOwnedById(
                                    session.id(),
                                    userId
                            )
                            .orElseThrow();

            int storedValue =
                    authoritative
                            .questionnaireResponse()
                            .answers()
                            .getFirst()
                            .value();

            assertTrue(
                    storedValue == 4
                            || storedValue == 5
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean concurrentUpdate(
            AssessmentSessionId sessionId,
            UserId userId,
            int value,
            CountDownLatch loaded,
            CountDownLatch startUpdate
    ) throws Exception {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );

        try {
            transactionTemplate.executeWithoutResult(
                    status -> {
                        AssessmentSession session =
                                sessionRepository
                                        .findOwnedById(
                                                sessionId,
                                                userId
                                        )
                                        .orElseThrow();

                        session.replaceQuestionnaireResponse(
                                new QuestionnaireResponse(
                                        List.of(
                                                new Answer(
                                                        new QuestionId("Q1"),
                                                        value
                                                )
                                        )
                                )
                        );

                        loaded.countDown();

                        await(
                                startUpdate
                        );

                        sessionRepository.update(
                                session
                        );
                    }
            );

            return true;

        } catch (
                OptimisticLockingFailureException exception
        ) {
            return false;
        }
    }

    private void await(
            CountDownLatch latch
    ) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "test thread interrupted",
                    exception
            );
        }
    }

    private Long databaseVersion(
            AssessmentSessionId sessionId
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM assessment_sessions
                WHERE id = ?
                """,
                Long.class,
                sessionId.value()
        );
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
                "questionnaire-test-"
                        + id
                        + "@example.com",
                "Questionnaire Test User",
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
