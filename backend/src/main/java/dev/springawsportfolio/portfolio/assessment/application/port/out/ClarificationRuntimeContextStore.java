package dev.springawsportfolio.portfolio.assessment.application.port.out;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeContext;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationExecutionToken;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;

import java.time.Instant;

public interface ClarificationRuntimeContextStore {

    ClarificationRuntimeContext loadActive(
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            ClarificationExecutionToken executionToken
    );

    boolean appendIfLatestSequenceMatches(
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            ClarificationExecutionToken executionToken,
            int expectedLatestSequence,
            ClarificationRuntimeMessage message
    );

    void deleteExecution(
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            ClarificationExecutionToken executionToken
    );

    int deleteExpired(
            Instant cutoff
    );
}
