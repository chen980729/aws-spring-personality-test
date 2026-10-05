package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.adapter;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakOptionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakQuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionTieBreakRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.tiebreak.DimensionTieBreak;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class PostgresDimensionTieBreakRepositoryAdapter
        implements DimensionTieBreakRepository {

    private static final String FIND_ONE_SQL =
            """
            SELECT
                session_id,
                dimension_code,
                question_id,
                selected_option_id,
                selected_pole,
                decided_at
            FROM assessment_dimension_tie_breaks
            WHERE session_id = ?
              AND dimension_code = ?
            """;

    private static final String FIND_ALL_SQL =
            """
            SELECT
                session_id,
                dimension_code,
                question_id,
                selected_option_id,
                selected_pole,
                decided_at
            FROM assessment_dimension_tie_breaks
            WHERE session_id = ?
            ORDER BY dimension_code
            """;

    private static final String INSERT_SQL =
            """
            INSERT INTO assessment_dimension_tie_breaks (
                session_id,
                dimension_code,
                question_id,
                selected_option_id,
                selected_pole,
                decided_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresDimensionTieBreakRepositoryAdapter(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate =
                Objects.requireNonNull(
                        jdbcTemplate,
                        "jdbcTemplate must not be null"
                );
    }

    @Override
    public Optional<DimensionTieBreak> findBySessionIdAndDimension(
            AssessmentSessionId sessionId,
            DimensionCode dimension
    ) {
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
        );

        List<DimensionTieBreak> results =
                jdbcTemplate.query(
                        FIND_ONE_SQL,
                        (resultSet, rowNumber) ->
                                mapTieBreak(
                                        resultSet
                                ),
                        sessionId.value(),
                        dimension.value()
                );

        return results
                .stream()
                .findFirst();
    }

    @Override
    public List<DimensionTieBreak> findBySessionId(
            AssessmentSessionId sessionId
    ) {
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        return jdbcTemplate.query(
                FIND_ALL_SQL,
                (resultSet, rowNumber) ->
                        mapTieBreak(
                                resultSet
                        ),
                sessionId.value()
        );
    }

    @Override
    public void add(
            DimensionTieBreak tieBreak
    ) {
        Objects.requireNonNull(
                tieBreak,
                "tieBreak must not be null"
        );

        jdbcTemplate.update(
                INSERT_SQL,
                tieBreak.sessionId().value(),
                tieBreak.dimension().value(),
                tieBreak.questionId() == null
                        ? null
                        : tieBreak.questionId().value(),
                tieBreak.selectedOptionId() == null
                        ? null
                        : tieBreak.selectedOptionId().value(),
                tieBreak.resolvedPole().value(),
                Timestamp.from(
                        tieBreak.decidedAt()
                )
        );
    }

    private static DimensionTieBreak mapTieBreak(
            java.sql.ResultSet resultSet
    ) throws java.sql.SQLException {
        return new DimensionTieBreak(
                new AssessmentSessionId(
                        resultSet.getObject(
                                "session_id",
                                java.util.UUID.class
                        )
                ),
                new DimensionCode(
                        resultSet.getString(
                                "dimension_code"
                        )
                ),
                nullableQuestionId(
                        resultSet.getString(
                                "question_id"
                        )
                ),
                nullableOptionId(
                        resultSet.getString(
                                "selected_option_id"
                        )
                ),
                new PoleCode(
                        resultSet.getString(
                                "selected_pole"
                        )
                ),
                resultSet
                        .getTimestamp(
                                "decided_at"
                        )
                        .toInstant()
        );
    }

    private static TieBreakQuestionId nullableQuestionId(
            String value
    ) {
        return value == null
                ? null
                : new TieBreakQuestionId(
                        value
                );
    }

    private static TieBreakOptionId nullableOptionId(
            String value
    ) {
        return value == null
                ? null
                : new TieBreakOptionId(
                        value
                );
    }
}
