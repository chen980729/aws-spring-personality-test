package dev.springawsportfolio.portfolio.assessment.web.clarification.dto;

import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;

import java.util.UUID;

/**
 * Public projection only. No provider details, execution token, or transcript.
 */
public record ClarificationInteractionResponse(
        UUID sessionId,
        String dimensionCode,
        String clarificationStatus,
        Integer assistantSequence,
        String question,
        AssessmentSessionResponse.ClarificationResultResponse result,
        AssessmentSessionStatus sessionStatus,
        AssessmentSessionResponse.AssessmentWorkflowResponse workflow
) {
}
