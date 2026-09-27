package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.query;

import dev.springawsportfolio.portfolio.assessment.application.port.out.AssessmentHistoryQuery;
import dev.springawsportfolio.portfolio.assessment.application.query.history.AssessmentHistoryPage;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class AssessmentHistoryQueryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:18"
            );

    @Autowired
    AssessmentHistoryQuery historyQuery;

    @Autowired
    AssessmentDefinitionRepository
            definitionRepository;

    @Autowired
    AssessmentDefinitionVersionRepository
            versionRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void returnsOnlyOwnersCompletedHistoryInDescendingCompletionOrder()
            throws Exception {

        UserId owner =
                createUser(
                        "history-owner"
                );

        UserId otherUser =
                createUser(
                        "history-other"
                );

        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                availableVersion(
                        definition
                );

        Instant base =
                Instant.parse(
                        "2026-09-27T00:00:00Z"
                );

        UUID olderSessionId =
                insertCompletedSession(
                        owner,
                        definition,
                        version,
                        "INFP",
                        base.plusSeconds(60)
                );

        UUID newerSessionId =
                insertCompletedSession(
                        owner,
                        definition,
                        version,
                        "INTJ",
                        base.plusSeconds(120)
                );

        insertCompletedSession(
                otherUser,
                definition,
                version,
                "ENTP",
                base.plusSeconds(180)
        );

        insertAbandonedSession(
                owner,
                definition,
                version,
                base.plusSeconds(240)
        );

        AssessmentHistoryPage firstPage =
                historyQuery.findCompletedByOwner(
                        owner,
                        1,
                        1
                );

        assertEquals(
                1,
                firstPage.items().size()
        );

        assertEquals(
                newerSessionId,
                firstPage
                        .items()
                        .getFirst()
                        .sessionId()
        );

        assertEquals(
                "INTJ",
                firstPage
                        .items()
                        .getFirst()
                        .finalType()
        );

        assertEquals(
                "SIXTEEN_PERSONALITY",
                firstPage
                        .items()
                        .getFirst()
                        .assessmentCode()
        );

        assertEquals(
                version.versionCode(),
                firstPage
                        .items()
                        .getFirst()
                        .assessmentVersion()
        );

        assertEquals(
                2,
                firstPage.totalElements()
        );

        assertEquals(
                2,
                firstPage.totalPages()
        );

        assertEquals(
                1,
                firstPage.page()
        );

        assertEquals(
                1,
                firstPage.size()
        );

        AssessmentHistoryPage secondPage =
                historyQuery.findCompletedByOwner(
                        owner,
                        2,
                        1
                );

        assertEquals(
                1,
                secondPage.items().size()
        );

        assertEquals(
                olderSessionId,
                secondPage
                        .items()
                        .getFirst()
                        .sessionId()
        );

        assertEquals(
                "INFP",
                secondPage
                        .items()
                        .getFirst()
                        .finalType()
        );
    }

    @Test
    void pageBeyondAvailableHistoryIsEmptyButKeepsPageMetadata() {
        UserId owner =
                createUser(
                        "history-empty-page"
                );

        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                availableVersion(
                        definition
                );

        insertCompletedSession(
                owner,
                definition,
                version,
                "INFJ",
                Instant.parse(
                        "2026-09-27T01:00:00Z"
                )
        );

        AssessmentHistoryPage page =
                historyQuery.findCompletedByOwner(
                        owner,
                        3,
                        20
                );

        assertTrue(
                page.items().isEmpty()
        );

        assertEquals(
                1,
                page.totalElements()
        );

        assertEquals(
                1,
                page.totalPages()
        );

        assertEquals(
                3,
                page.page()
        );

        assertEquals(
                20,
                page.size()
        );
    }

    private UUID insertCompletedSession(
            UserId owner,
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version,
            String finalType,
            Instant completedAt
    ) {
        UUID sessionId =
                UUID.randomUUID();

        Instant createdAt =
                completedAt.minusSeconds(
                        300
                );

        jdbcTemplate.update(
                """
                INSERT INTO assessment_sessions (
                    id,
                    user_id,
                    definition_id,
                    definition_version_id,
                    status,
                    questionnaire_response,
                    questionnaire_submitted_at,
                    initial_result,
                    final_result,
                    created_at,
                    completed_at,
                    version
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    'COMPLETED',
                    ?::jsonb,
                    ?,
                    ?::jsonb,
                    ?::jsonb,
                    ?,
                    ?,
                    0
                )
                """,
                sessionId,
                owner.value(),
                definition.id().value(),
                version.id().value(),
                """
                {
                  "answers": []
                }
                """,
                Timestamp.from(
                        completedAt
                                .minusSeconds(60)
                ),
                """
                {
                  "dimensions": []
                }
                """,
                """
                {
                  "finalType": "%s",
                  "dimensions": []
                }
                """.formatted(
                        finalType
                ),
                Timestamp.from(
                        createdAt
                ),
                Timestamp.from(
                        completedAt
                )
        );

        return sessionId;
    }

    private UUID insertAbandonedSession(
            UserId owner,
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version,
            Instant abandonedAt
    ) {
        UUID sessionId =
                UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO assessment_sessions (
                    id,
                    user_id,
                    definition_id,
                    definition_version_id,
                    status,
                    created_at,
                    abandoned_at,
                    version
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    'ABANDONED',
                    ?,
                    ?,
                    0
                )
                """,
                sessionId,
                owner.value(),
                definition.id().value(),
                version.id().value(),
                Timestamp.from(
                        abandonedAt
                                .minusSeconds(300)
                ),
                Timestamp.from(
                        abandonedAt
                )
        );

        return sessionId;
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

    private UserId createUser(
            String prefix
    ) {
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
                prefix
                        + "-"
                        + id
                        + "@example.com",
                "Assessment History Test User",
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
