package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.util.Objects;

public record ContinueDimensionClarificationCommand(
        UserId actorUserId,
        AssessmentSessionId sessionId,
        DimensionCode dimension,
        String answer
) {

    public ContinueDimensionClarificationCommand {
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
                answer,
                "answer must not be null"
        );

        answer = answer.trim();

        if (answer.isEmpty()) {
            throw new IllegalArgumentException(
                    "answer must not be blank"
            );
        }
    }
}
