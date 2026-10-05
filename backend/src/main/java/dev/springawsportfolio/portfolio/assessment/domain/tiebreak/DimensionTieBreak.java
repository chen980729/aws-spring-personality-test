package dev.springawsportfolio.portfolio.assessment.domain.tiebreak;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakOptionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakQuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;

import java.time.Instant;
import java.util.Objects;

public record DimensionTieBreak(
        AssessmentSessionId sessionId,
        DimensionCode dimension,
        TieBreakQuestionId questionId,
        TieBreakOptionId selectedOptionId,
        PoleCode resolvedPole,
        Instant decidedAt
) {

    public DimensionTieBreak(
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            PoleCode selectedPole,
            Instant decidedAt
    ) {
        this(
                sessionId,
                dimension,
                null,
                null,
                selectedPole,
                decidedAt
        );
    }

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
                resolvedPole,
                "resolvedPole must not be null"
        );

        Objects.requireNonNull(
                decidedAt,
                "decidedAt must not be null"
        );

        if (
                (questionId == null)
                        != (selectedOptionId == null)
        ) {
            throw new IllegalArgumentException(
                    "questionId and selectedOptionId must both "
                            + "be present or both be absent"
            );
        }
    }

    public boolean isLegacyDirectSelection() {
        return questionId == null;
    }

    public boolean isContextualQuestionSelection() {
        return questionId != null;
    }

    public PoleCode selectedPole() {
        return resolvedPole;
    }
}
