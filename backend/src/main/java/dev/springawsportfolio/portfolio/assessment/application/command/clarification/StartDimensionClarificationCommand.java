package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.util.Objects;

public record StartDimensionClarificationCommand(
        UserId actorUserId,
        AssessmentSessionId sessionId,
        DimensionCode dimension
) {

    public StartDimensionClarificationCommand {
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
    }
}
