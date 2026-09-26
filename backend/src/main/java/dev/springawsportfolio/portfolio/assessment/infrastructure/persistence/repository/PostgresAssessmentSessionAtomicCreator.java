package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository;

import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public final class PostgresAssessmentSessionAtomicCreator {

    //requiring PostgreSQL atomic semantics
    private static final String INSERT_SQL = """
            INSERT INTO assessment_sessions (
                id,
                user_id,
                definition_id,
                definition_version_id,
                status,
                created_at,
                version
            )
            VALUES (?, ?, ?, ?, ?, ?, 0)
            ON CONFLICT (user_id, definition_id)
            WHERE status IN (
                'IN_PROGRESS',
                'AWAITING_CLARIFICATION',
                'CLARIFICATION_IN_PROGRESS'
            )
            DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresAssessmentSessionAtomicCreator(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tryCreate(
            AssessmentSession session
    ) {
        int affectedRows =
                jdbcTemplate.update(
                        INSERT_SQL,
                        session.id().value(),
                        session.ownerUserId().value(),
                        session.definitionId().value(),
                        session.definitionVersionId().value(),
                        session.status().name(),
                        Timestamp.from(
                                session.createdAt()
                        )
                );

        return affectedRows == 1;
    }
}
