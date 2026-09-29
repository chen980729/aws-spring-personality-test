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
        PoleCode selectedPole
) {

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
                selectedPole,
                "selectedPole must not be null"
        );
    }
}
