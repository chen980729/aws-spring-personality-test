package dev.springawsportfolio.portfolio.assessment.domain.tiebreak;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;

import java.time.Instant;
import java.util.Objects;

public record DimensionTieBreak(
        AssessmentSessionId sessionId,
        DimensionCode dimension,
        PoleCode selectedPole,
        Instant decidedAt
) {

    public DimensionTieBreak {
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
        );

        Objects.requireNonNull(
                selectedPole,
                "selectedPole must not be null"
        );

        Objects.requireNonNull(
                decidedAt,
                "decidedAt must not be null"
        );
    }
}
