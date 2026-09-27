package dev.springawsportfolio.portfolio.assessment.application.port.out;

import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionWorkflowSnapshot;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;

public interface AssessmentSessionWorkflowQuery {

    AssessmentSessionWorkflowSnapshot findBySessionId(
            AssessmentSessionId sessionId
    );
}
