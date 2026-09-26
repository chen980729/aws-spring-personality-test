package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class JpaAssessmentSessionRepositoryAdapterIntegrationTest {

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

    @Test
    void createsAndLoadsActiveSession() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                availableVersion(
                        definition
                );

        AssessmentSession candidate =
                newSession(
                        userId,
                        definition,
                        version
                );

        boolean created =
                sessionRepository.tryCreateActive(
                        candidate
                );

        assertTrue(created);

        AssessmentSession loaded =
                sessionRepository
                        .findActive(
                                userId,
                                definition.id()
                        )
                        .orElseThrow();

        assertEquals(
                candidate.id(),
                loaded.id()
        );

        assertEquals(
                userId,
                loaded.ownerUserId()
        );

        assertEquals(
                definition.id(),
                loaded.definitionId()
        );

        assertEquals(
                version.id(),
                loaded.definitionVersionId()
        );

        assertTrue(
                loaded.isActive()
        );

        assertTrue(
                loaded
                        .questionnaireResponse()
                        .answers()
                        .isEmpty()
        );

        assertFalse(
                loaded.isQuestionnaireSubmitted()
        );
    }

    @Test
    void findsSessionOnlyForItsOwner() {
        UserId owner =
                createUser();

        UserId anotherUser =
                createUser();

        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                availableVersion(
                        definition
                );

        AssessmentSession session =
                newSession(
                        owner,
                        definition,
                        version
                );

        assertTrue(
                sessionRepository.tryCreateActive(
                        session
                )
        );

        assertTrue(
                sessionRepository
                        .findOwnedById(
                                session.id(),
                                owner
                        )
                        .isPresent()
        );

        assertTrue(
                sessionRepository
                        .findOwnedById(
                                session.id(),
                                anotherUser
                        )
                        .isEmpty()
        );
    }

    @Test
    void doesNotCreateSecondActiveSession() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                availableVersion(
                        definition
                );

        AssessmentSession first =
                newSession(
                        userId,
                        definition,
                        version
                );

        AssessmentSession second =
                newSession(
                        userId,
                        definition,
                        version
                );

        assertTrue(
                sessionRepository.tryCreateActive(
                        first
                )
        );

        assertFalse(
                sessionRepository.tryCreateActive(
                        second
                )
        );

        AssessmentSession active =
                sessionRepository
                        .findActive(
                                userId,
                                definition.id()
                        )
                        .orElseThrow();

        assertEquals(
                first.id(),
                active.id()
        );
    }

    @Test
    void concurrentCreationProducesExactlyOneActiveSession()
            throws Exception {

        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                availableVersion(
                        definition
                );

        AssessmentSession first =
                newSession(
                        userId,
                        definition,
                        version
                );

        AssessmentSession second =
                newSession(
                        userId,
                        definition,
                        version
                );

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> firstResult =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                return sessionRepository
                                        .tryCreateActive(
                                                first
                                        );
                            }
                    );

            Future<Boolean> secondResult =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                return sessionRepository
                                        .tryCreateActive(
                                                second
                                        );
                            }
                    );

            ready.await();
            start.countDown();

            boolean firstCreated =
                    firstResult.get();

            boolean secondCreated =
                    secondResult.get();

            assertEquals(
                    1,
                    (firstCreated ? 1 : 0)
                            + (secondCreated ? 1 : 0)
            );

            AssessmentSession active =
                    sessionRepository
                            .findActive(
                                    userId,
                                    definition.id()
                            )
                            .orElseThrow();

            assertTrue(
                    active.id().equals(first.id())
                            || active.id().equals(second.id())
            );
        } finally {
            executor.shutdownNow();
        }
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

    private AssessmentSession newSession(
            UserId userId,
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version
    ) {
        return AssessmentSession.start(
                AssessmentSessionId.newId(),
                userId,
                definition.id(),
                version.id(),
                Instant.now()
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
                "assessment-test-"
                        + id
                        + "@example.com",
                "Assessment Test User",
                "test-password-hash",
                Timestamp.from(
                        Instant.now()
                )
        );

        return new UserId(id);
    }
}
