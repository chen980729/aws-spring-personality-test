package dev.springawsportfolio.portfolio.assessment.application.command.submission;

import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SubmitQuestionnaireResult(
        AssessmentSessionResult session
) {

    public SubmitQuestionnaireResult {
        Objects.requireNonNull(
                session,
                "session must not be null"
        );
    }

    /*
     * Convenience accessors preserve the existing Application /
     * integration-test API while the HTTP layer can consume
     * the complete Session projection.
     */

    public UUID sessionId() {
        return session.id();
    }

    public AssessmentSessionStatus status() {
        return session.status();
    }

    public Instant questionnaireSubmittedAt() {
        return session.questionnaireSubmittedAt();
    }

    public InitialAssessmentResult initialResult() {
        return session.initialResult();
    }

    public FinalAssessmentResult finalResult() {
        return session.finalResult();
    }

    public List<DimensionCode>
    pendingClarificationDimensions() {
        return session
                .workflow()
                .pendingClarificationDimensions()
                .stream()
                .map(
                        DimensionCode::new
                )
                .toList();
    }

    public Instant completedAt() {
        return session.completedAt();
    }
}