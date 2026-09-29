package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationExecutionToken;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.util.Objects;

/**
 * Internal correlation value for one active clarification execution.
 *
 * It is deliberately not a public HTTP token. Step 8 will carry this value
 * across the external AI call so transaction B can reject stale work after
 * Skip, Retry, Restart, completion, or another lifecycle change.
 */
public record ClarificationExecutionTicket(
        UserId actorUserId,
        AssessmentSessionId sessionId,
        DimensionCode dimension,
        ClarificationExecutionToken executionToken
) {

    public ClarificationExecutionTicket {
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
                executionToken,
                "executionToken must not be null"
        );
    }
}
