package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.adapter;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeContext;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationExecutionToken;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
public class PostgresClarificationRuntimeContextStore
        implements ClarificationRuntimeContextStore {

    private static final String LOAD_ACTIVE_SQL =
            """
            SELECT
                message.sequence_number,
                message.message_role,
                message.message_text,
                message.created_at,
                message.expires_at
            FROM assessment_clarification_runtime_messages message
            JOIN assessment_dimension_clarifications clarification
              ON clarification.id = message.clarification_id
            WHERE clarification.session_id = ?
              AND clarification.dimension_code = ?
              AND clarification.status = 'IN_PROGRESS'
              AND clarification.active_execution_token = ?
              AND message.execution_token = ?
            ORDER BY message.sequence_number
            """;

    private static final String REFRESH_EXPIRY_SQL =
            """
            UPDATE assessment_clarification_runtime_messages message
            SET expires_at = ?
            FROM assessment_dimension_clarifications clarification
            WHERE message.clarification_id = clarification.id
              AND message.execution_token = ?
              AND clarification.session_id = ?
              AND clarification.dimension_code = ?
              AND clarification.status = 'IN_PROGRESS'
              AND clarification.active_execution_token = ?
              AND COALESCE(
                    (
                        SELECT MAX(existing.sequence_number)
                        FROM assessment_clarification_runtime_messages existing
                        WHERE existing.clarification_id = clarification.id
                          AND existing.execution_token = ?
                    ),
                    0
                  ) = ?
            """;

    private static final String APPEND_SQL =
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
            SELECT
                clarification.id,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?
            FROM assessment_dimension_clarifications clarification
            WHERE clarification.session_id = ?
              AND clarification.dimension_code = ?
              AND clarification.status = 'IN_PROGRESS'
              AND clarification.active_execution_token = ?
              AND COALESCE(
                    (
                        SELECT MAX(existing.sequence_number)
                        FROM assessment_clarification_runtime_messages existing
                        WHERE existing.clarification_id = clarification.id
                          AND existing.execution_token = ?
                    ),
                    0
                  ) = ?
            ON CONFLICT DO NOTHING
            """;

    private static final String DELETE_EXECUTION_SQL =
            """
            DELETE FROM assessment_clarification_runtime_messages message
            USING assessment_dimension_clarifications clarification
            WHERE message.clarification_id = clarification.id
              AND clarification.session_id = ?
              AND clarification.dimension_code = ?
              AND message.execution_token = ?
            """;

    private static final String DELETE_EXPIRED_SQL =
            """
            DELETE FROM assessment_clarification_runtime_messages
            WHERE expires_at <= ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresClarificationRuntimeContextStore(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate =
                Objects.requireNonNull(
                        jdbcTemplate,
                        "jdbcTemplate must not be null"
                );
    }

    @Override
    public ClarificationRuntimeContext loadActive(
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            ClarificationExecutionToken executionToken
    ) {
        requireIdentity(
                sessionId,
                dimension,
                executionToken
        );

        List<ClarificationRuntimeMessage> messages =
                jdbcTemplate.query(
                        LOAD_ACTIVE_SQL,
                        (resultSet, rowNumber) ->
                                new ClarificationRuntimeMessage(
                                        resultSet.getInt(
                                                "sequence_number"
                                        ),
                                        ClarificationRuntimeMessage.Role
                                                .valueOf(
                                                        resultSet.getString(
                                                                "message_role"
                                                        )
                                                ),
                                        resultSet.getString(
                                                "message_text"
                                        ),
                                        resultSet
                                                .getTimestamp(
                                                        "created_at"
                                                )
                                                .toInstant(),
                                        resultSet
                                                .getTimestamp(
                                                        "expires_at"
                                                )
                                                .toInstant()
                                ),
                        sessionId.value(),
                        dimension.value(),
                        executionToken.value(),
                        executionToken.value()
                );

        return new ClarificationRuntimeContext(
                messages
        );
    }

    @Override
    @Transactional
    public boolean appendIfLatestSequenceMatches(
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            ClarificationExecutionToken executionToken,
            int expectedLatestSequence,
            ClarificationRuntimeMessage message
    ) {
        requireIdentity(
                sessionId,
                dimension,
                executionToken
        );

        Objects.requireNonNull(
                message,
                "message must not be null"
        );

        if (expectedLatestSequence < 0) {
            throw new IllegalArgumentException(
                    "expectedLatestSequence must not be negative"
            );
        }

        if (
                message.sequenceNumber()
                        != expectedLatestSequence + 1
        ) {
            throw new IllegalArgumentException(
                    "message sequence must immediately follow "
                            + "expectedLatestSequence"
            );
        }

        Timestamp expiresAt =
                Timestamp.from(
                        message.expiresAt()
                );

        jdbcTemplate.update(
                REFRESH_EXPIRY_SQL,
                expiresAt,
                executionToken.value(),
                sessionId.value(),
                dimension.value(),
                executionToken.value(),
                executionToken.value(),
                expectedLatestSequence
        );

        int inserted =
                jdbcTemplate.update(
                        APPEND_SQL,
                        executionToken.value(),
                        message.sequenceNumber(),
                        message.role().name(),
                        message.text(),
                        Timestamp.from(
                                message.createdAt()
                        ),
                        expiresAt,
                        sessionId.value(),
                        dimension.value(),
                        executionToken.value(),
                        executionToken.value(),
                        expectedLatestSequence
                );

        return inserted == 1;
    }

    @Override
    public void deleteExecution(
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            ClarificationExecutionToken executionToken
    ) {
        requireIdentity(
                sessionId,
                dimension,
                executionToken
        );

        jdbcTemplate.update(
                DELETE_EXECUTION_SQL,
                sessionId.value(),
                dimension.value(),
                executionToken.value()
        );
    }

    @Override
    public int deleteExpired(
            Instant cutoff
    ) {
        Objects.requireNonNull(
                cutoff,
                "cutoff must not be null"
        );

        return jdbcTemplate.update(
                DELETE_EXPIRED_SQL,
                Timestamp.from(
                        cutoff
                )
        );
    }

    private static void requireIdentity(
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            ClarificationExecutionToken executionToken
    ) {
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );
        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
        );
        Objects.requireNonNull(
                executionToken,
                "executionToken must not be null"
        );
    }
}
