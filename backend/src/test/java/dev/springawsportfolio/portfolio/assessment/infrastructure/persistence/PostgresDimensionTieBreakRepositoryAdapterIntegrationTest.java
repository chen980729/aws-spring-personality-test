package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakOptionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakQuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionTieBreakRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.tiebreak.DimensionTieBreak;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class PostgresDimensionTieBreakRepositoryAdapterIntegrationTest {

    private static final AssessmentDefinitionVersionId
            VERSION_1_1_ID =
            new AssessmentDefinitionVersionId(
                    UUID.fromString(
                            "b7a63f1e-60f4-4b5e-a0e3-1c1b1a110001"
                    )
            );

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    AssessmentDefinitionRepository definitionRepository;

    @Autowired
    AssessmentDefinitionVersionRepository versionRepository;

    @Autowired
    AssessmentSessionRepository sessionRepository;

    @Autowired
    DimensionTieBreakRepository tieBreakRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void roundTripsLegacyAndContextualTieBreakFacts() {
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
                        .findById(
                                VERSION_1_1_ID
                        )
                        .orElseThrow();

        AssessmentSession session =
                createSession(
                        userId,
                        definition,
                        version
                );

        List<DimensionDefinition> dimensions =
                version
                        .specification()
                        .dimensions();

        DimensionDefinition contextualDimension =
                dimensions.getFirst();

        DimensionDefinition legacyDimension =
                dimensions.get(1);

        Instant contextualDecidedAt =
                Instant.parse(
                        "2026-10-05T10:00:00Z"
                );

        Instant legacyDecidedAt =
                Instant.parse(
                        "2026-10-05T10:01:00Z"
                );

        tieBreakRepository.add(
                new DimensionTieBreak(
                        session.id(),
                        contextualDimension.code(),
                        new TieBreakQuestionId(
                                "TB-EI-1"
                        ),
                        new TieBreakOptionId(
                                "TB-EI-02"
                        ),
                        contextualDimension.poleB(),
                        contextualDecidedAt
                )
        );

        tieBreakRepository.add(
                new DimensionTieBreak(
                        session.id(),
                        legacyDimension.code(),
                        legacyDimension.poleA(),
                        legacyDecidedAt
                )
        );

        DimensionTieBreak contextual =
                tieBreakRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                contextualDimension.code()
                        )
                        .orElseThrow();

        assertTrue(
                contextual.isContextualQuestionSelection()
        );

        assertEquals(
                "TB-EI-1",
                contextual
                        .questionId()
                        .value()
        );

        assertEquals(
                "TB-EI-02",
                contextual
                        .selectedOptionId()
                        .value()
        );

        assertEquals(
                contextualDimension.poleB(),
                contextual.resolvedPole()
        );

        assertEquals(
                contextualDecidedAt,
                contextual.decidedAt()
        );

        DimensionTieBreak legacy =
                tieBreakRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                legacyDimension.code()
                        )
                        .orElseThrow();

        assertTrue(
                legacy.isLegacyDirectSelection()
        );

        assertNull(
                legacy.questionId()
        );

        assertNull(
                legacy.selectedOptionId()
        );

        assertEquals(
                legacyDimension.poleA(),
                legacy.resolvedPole()
        );

        assertEquals(
                2,
                tieBreakRepository
                        .findBySessionId(
                                session.id()
                        )
                        .size()
        );
    }

    @Test
    void databaseRejectsHalfPopulatedContextualProvenance() {
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
                        .findById(
                                VERSION_1_1_ID
                        )
                        .orElseThrow();

        AssessmentSession session =
                createSession(
                        userId,
                        definition,
                        version
                );

        DimensionDefinition dimension =
                version
                        .specification()
                        .dimensions()
                        .get(2);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO assessment_dimension_tie_breaks (
                            session_id,
                            dimension_code,
                            question_id,
                            selected_option_id,
                            selected_pole,
                            decided_at
                        )
                        VALUES (?, ?, ?, NULL, ?, ?)
                        """,
                        session.id().value(),
                        dimension.code().value(),
                        "TB-TF-1",
                        dimension.poleA().value(),
                        Timestamp.from(
                                Instant.parse(
                                        "2026-10-05T10:02:00Z"
                                )
                        )
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
                "tie-break-persistence-"
                        + id
                        + "@example.com",
                "Tie Break Persistence User",
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
