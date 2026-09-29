package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.util.Objects;

public record SkipRemainingClarificationsCommand(
        UserId actorUserId,
        AssessmentSessionId sessionId
) {

    public SkipRemainingClarificationsCommand {
        Objects.requireNonNull(
                actorUserId,
                "actorUserId must not be null"
        );

        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );
    }
}
