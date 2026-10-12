package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.util.Objects;

public record ContinueDimensionClarificationCommand(
        UserId actorUserId,
        AssessmentSessionId sessionId,
        DimensionCode dimension,
        int expectedAssistantSequence,
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

        if (
                expectedAssistantSequence <= 0
                        || expectedAssistantSequence % 2 == 0
        ) {
            throw new IllegalArgumentException(
                    "expectedAssistantSequence must be a positive odd number"
            );
        }

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

        int codePoints =
                answer.codePointCount(
                        0,
                        answer.length()
                );

        if (
                codePoints
                        > ClarificationRuntimeMessage
                        .MAX_USER_ANSWER_CODE_POINTS
        ) {
            throw new IllegalArgumentException(
                    "answer exceeds maximum length of "
                            + ClarificationRuntimeMessage
                            .MAX_USER_ANSWER_CODE_POINTS
                            + " Unicode code points"
            );
        }
    }
}
