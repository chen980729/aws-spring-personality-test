package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.query;

import dev.springawsportfolio.portfolio.assessment.application.port.out.AssessmentSessionWorkflowQuery;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionWorkflowSnapshot;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
public class PostgresAssessmentSessionWorkflowQuery
        implements AssessmentSessionWorkflowQuery {

    private static final String CLARIFICATION_SQL =
            """
            SELECT
                dimension_code,
                status,
                result_outcome,
                suggested_pole,
                result_confidence,
                result_summary,
                started_at,
                accepted_at
            FROM assessment_dimension_clarifications
            WHERE session_id = ?
            ORDER BY dimension_code
            """;

    private static final String TIE_BREAK_SQL =
            """
            SELECT
                dimension_code,
                question_id,
                selected_option_id,
                selected_pole,
                decided_at
            FROM assessment_dimension_tie_breaks
            WHERE session_id = ?
            ORDER BY dimension_code
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresAssessmentSessionWorkflowQuery(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate =
                Objects.requireNonNull(
                        jdbcTemplate,
                        "jdbcTemplate must not be null"
                );
    }

    @Override
    public AssessmentSessionWorkflowSnapshot findBySessionId(
            AssessmentSessionId sessionId
    ) {
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        List<AssessmentSessionWorkflowSnapshot.ClarificationSnapshot>
                clarifications =
                jdbcTemplate.query(
                        CLARIFICATION_SQL,
                        (resultSet, rowNumber) -> {
                            String resultOutcome =
                                    resultSet.getString(
                                            "result_outcome"
                                    );

                            return new AssessmentSessionWorkflowSnapshot
                                    .ClarificationSnapshot(
                                    resultSet.getString(
                                            "dimension_code"
                                    ),
                                    DimensionClarificationStatus.valueOf(
                                            resultSet.getString(
                                                    "status"
                                            )
                                    ),
                                    resultOutcome == null
                                            ? null
                                            : AssessmentSessionWorkflowSnapshot
                                            .ClarificationResolution
                                            .valueOf(
                                                    resultOutcome
                                            ),
                                    resultSet.getString(
                                            "suggested_pole"
                                    ),
                                    resultSet.getString(
                                            "result_confidence"
                                    ),
                                    resultSet.getString(
                                            "result_summary"
                                    ),
                                    toInstant(
                                            resultSet.getTimestamp(
                                                    "started_at"
                                            )
                                    ),
                                    toInstant(
                                            resultSet.getTimestamp(
                                                    "accepted_at"
                                            )
                                    )
                            );
                        },
                        sessionId.value()
                );

        List<AssessmentSessionWorkflowSnapshot.TieBreakSnapshot>
                tieBreaks =
                jdbcTemplate.query(
                        TIE_BREAK_SQL,
                        (resultSet, rowNumber) ->
                                new AssessmentSessionWorkflowSnapshot
                                        .TieBreakSnapshot(
                                        resultSet.getString(
                                                "dimension_code"
                                        ),
                                        resultSet.getString(
                                                "question_id"
                                        ),
                                        resultSet.getString(
                                                "selected_option_id"
                                        ),
                                        resultSet.getString(
                                                "selected_pole"
                                        ),
                                        Objects.requireNonNull(
                                                toInstant(
                                                        resultSet.getTimestamp(
                                                                "decided_at"
                                                        )
                                                ),
                                                "persisted tie-break decidedAt "
                                                        + "must not be null"
                                        )
                                ),
                        sessionId.value()
                );

        return new AssessmentSessionWorkflowSnapshot(
                clarifications,
                tieBreaks
        );
    }

    private static Instant toInstant(
            Timestamp timestamp
    ) {
        return timestamp == null
                ? null
                : timestamp.toInstant();
    }
}
