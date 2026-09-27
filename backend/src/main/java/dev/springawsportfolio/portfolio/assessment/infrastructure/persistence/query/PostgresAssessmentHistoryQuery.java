package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.query;

import dev.springawsportfolio.portfolio.assessment.application.port.out.AssessmentHistoryQuery;
import dev.springawsportfolio.portfolio.assessment.application.query.history.AssessmentHistoryItem;
import dev.springawsportfolio.portfolio.assessment.application.query.history.AssessmentHistoryPage;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class PostgresAssessmentHistoryQuery
        implements AssessmentHistoryQuery {

    private static final String LIST_SQL =
            """
            SELECT
                s.id AS session_id,
                d.code AS assessment_code,
                v.version_code AS assessment_version,
                s.final_result ->> 'finalType' AS final_type,
                s.completed_at AS completed_at
            FROM assessment_sessions s
            JOIN assessment_definitions d
              ON d.id = s.definition_id
            JOIN assessment_definition_versions v
              ON v.id = s.definition_version_id
             AND v.definition_id = s.definition_id
            WHERE s.user_id = ?
              AND s.status = 'COMPLETED'
            ORDER BY
                s.completed_at DESC,
                s.id DESC
            LIMIT ?
            OFFSET ?
            """;

    private static final String COUNT_SQL =
            """
            SELECT COUNT(*)
            FROM assessment_sessions s
            WHERE s.user_id = ?
              AND s.status = 'COMPLETED'
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresAssessmentHistoryQuery(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate =
                Objects.requireNonNull(
                        jdbcTemplate,
                        "jdbcTemplate must not be null"
                );
    }

    @Override
    public AssessmentHistoryPage findCompletedByOwner(
            UserId ownerUserId,
            int page,
            int size
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId must not be null"
        );

        long offset =
                (long) (page - 1)
                        * size;

        List<AssessmentHistoryItem> items =
                jdbcTemplate.query(
                        LIST_SQL,
                        (resultSet, rowNumber) -> {
                            UUID sessionId =
                                    resultSet.getObject(
                                            "session_id",
                                            UUID.class
                                    );

                            String assessmentCode =
                                    resultSet.getString(
                                            "assessment_code"
                                    );

                            String assessmentVersion =
                                    resultSet.getString(
                                            "assessment_version"
                                    );

                            String finalType =
                                    resultSet.getString(
                                            "final_type"
                                    );

                            Timestamp completedTimestamp =
                                    resultSet.getTimestamp(
                                            "completed_at"
                                    );

                            if (
                                    finalType == null
                                            || finalType.isBlank()
                            ) {
                                throw new IllegalStateException(
                                        "COMPLETED assessment session "
                                                + sessionId
                                                + " does not contain "
                                                + "a valid finalType"
                                );
                            }

                            if (completedTimestamp == null) {
                                throw new IllegalStateException(
                                        "COMPLETED assessment session "
                                                + sessionId
                                                + " does not contain completedAt"
                                );
                            }

                            Instant completedAt =
                                    completedTimestamp.toInstant();

                            return new AssessmentHistoryItem(
                                    sessionId,
                                    assessmentCode,
                                    assessmentVersion,
                                    finalType,
                                    completedAt
                            );
                        },
                        ownerUserId.value(),
                        size,
                        offset
                );

        Long total =
                jdbcTemplate.queryForObject(
                        COUNT_SQL,
                        Long.class,
                        ownerUserId.value()
                );

        long totalElements =
                total == null
                        ? 0L
                        : total;

        int totalPages =
                totalElements == 0
                        ? 0
                        : (int) (
                        (
                                totalElements
                                        + size
                                        - 1
                        )
                                / size
                );

        return new AssessmentHistoryPage(
                items,
                page,
                size,
                totalElements,
                totalPages
        );
    }
}