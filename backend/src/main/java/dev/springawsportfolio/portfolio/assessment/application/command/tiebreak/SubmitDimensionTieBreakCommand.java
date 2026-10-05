package dev.springawsportfolio.portfolio.assessment.application.command.tiebreak;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.util.Objects;

public record SubmitDimensionTieBreakCommand(
        UserId actorUserId,
        AssessmentSessionId sessionId,
        DimensionCode dimension,
        TieBreakSelection selection
) {

    public SubmitDimensionTieBreakCommand(
            UserId actorUserId,
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            PoleCode selectedPole
    ) {
        this(
                actorUserId,
                sessionId,
                dimension,
                new TieBreakSelection.DirectPoleSelection(
                        selectedPole
                )
        );
    }

    public SubmitDimensionTieBreakCommand {
        Objects.requireNonNull(
                actorUserId,
                "actorUserId must not be null"
        );

        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
        );

        Objects.requireNonNull(
                selection,
                "selection must not be null"
        );
    }
}
