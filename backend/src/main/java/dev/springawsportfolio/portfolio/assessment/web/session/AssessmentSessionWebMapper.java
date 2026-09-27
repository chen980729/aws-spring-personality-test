package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.start.StartAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireResult;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.SessionQuestionnaireResponse;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.StartAssessmentResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class AssessmentSessionWebMapper {

    public StartAssessmentResponse toStartResponse(
            StartAssessmentResult result
    ) {
        return new StartAssessmentResponse(
                result.created(),
                toSessionResponse(
                        result.session()
                )
        );
    }

    public AssessmentSessionResponse toSessionResponse(
            AssessmentSessionResult session
    ) {
        var answers =
                session
                        .answers()
                        .stream()
                        .map(answer ->
                                new AssessmentSessionResponse
                                        .QuestionAnswerResponse(
                                        answer.questionId(),
                                        answer.value()
                                )
                        )
                        .toList();

        return new AssessmentSessionResponse(
                session.id(),

                new AssessmentSessionResponse
                        .BoundAssessmentResponse(
                        session.assessmentCode(),
                        session.assessmentVersion()
                ),

                session.status(),

                new AssessmentSessionResponse
                        .QuestionnaireStateResponse(
                        answers,
                        session.questionnaireSubmitted(),
                        session.questionnaireSubmittedAt()
                ),

                mapInitialResult(
                        session
                ),

                mapClarifications(
                        session
                ),

                mapTieBreaks(
                        session
                ),

                mapFinalResult(
                        session
                ),

                new AssessmentSessionResponse
                        .AssessmentWorkflowResponse(
                        session
                                .workflow()
                                .pendingClarificationDimensions(),
                        session
                                .workflow()
                                .retryableClarificationDimensions(),
                        session
                                .workflow()
                                .tieBreakRequiredDimensions(),
                        session
                                .workflow()
                                .completed()
                ),

                session.createdAt(),
                session.completedAt(),
                session.abandonedAt()
        );
    }

    private AssessmentSessionResponse
            .InitialAssessmentResultResponse
    mapInitialResult(
            AssessmentSessionResult session
    ) {
        if (session.initialResult() == null) {
            return null;
        }

        List<AssessmentSessionResponse
                .InitialDimensionResultResponse>
                dimensions =
                session
                        .initialResult()
                        .dimensions()
                        .stream()
                        .map(dimension ->
                                mapInitialDimension(
                                        session,
                                        dimension
                                )
                        )
                        .toList();

        return new AssessmentSessionResponse
                .InitialAssessmentResultResponse(
                dimensions
        );
    }

    private AssessmentSessionResponse
            .InitialDimensionResultResponse
    mapInitialDimension(
            AssessmentSessionResult session,
            InitialDimensionResult dimension
    ) {
        AssessmentSessionResult.DimensionEvidenceResult
                evidence =
                session
                        .dimensionEvidence()
                        .stream()
                        .filter(candidate ->
                                candidate
                                        .dimensionCode()
                                        .equals(
                                                dimension
                                                        .dimension()
                                                        .value()
                                        )
                        )
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "missing questionnaire "
                                                        + "evidence for dimension: "
                                                        + dimension
                                                        .dimension()
                                                        .value()
                                        )
                        );

        return new AssessmentSessionResponse
                .InitialDimensionResultResponse(
                dimension
                        .dimension()
                        .value(),
                dimension.rawScore(),
                dimension
                        .questionnairePreference()
                        == null
                        ? null
                        : dimension
                        .questionnairePreference()
                        .value(),
                dimension.ambiguous(),

                new AssessmentSessionResponse
                        .QuestionnaireEvidenceResponse(
                        evidence.poleA(),
                        evidence.poleAPercentage(),
                        evidence.poleB(),
                        evidence.poleBPercentage()
                )
        );
    }

    private List<AssessmentSessionResponse
            .ClarificationStateResponse>
    mapClarifications(
            AssessmentSessionResult session
    ) {
        return session
                .clarifications()
                .stream()
                .map(clarification ->
                        new AssessmentSessionResponse
                                .ClarificationStateResponse(
                                clarification.dimensionCode(),
                                clarification.status().name(),
                                null,
                                clarification.startedAt(),
                                clarification.acceptedAt()
                        )
                )
                .toList();
    }

    private List<AssessmentSessionResponse
            .TieBreakStateResponse>
    mapTieBreaks(
            AssessmentSessionResult session
    ) {
        return session
                .tieBreaks()
                .stream()
                .map(tieBreak ->
                        new AssessmentSessionResponse
                                .TieBreakStateResponse(
                                tieBreak.dimensionCode(),
                                tieBreak.selectedPole(),
                                tieBreak.decidedAt()
                        )
                )
                .toList();
    }

    private AssessmentSessionResponse
            .FinalAssessmentResultResponse
    mapFinalResult(
            AssessmentSessionResult session
    ) {
        if (session.finalResult() == null) {
            return null;
        }

        var dimensions =
                session
                        .finalResult()
                        .dimensions()
                        .stream()
                        .map(dimension ->
                                mapFinalDimension(
                                        session,
                                        dimension
                                )
                        )
                        .toList();

        return new AssessmentSessionResponse
                .FinalAssessmentResultResponse(
                session
                        .finalResult()
                        .finalType(),
                dimensions
        );
    }

    private AssessmentSessionResponse
            .FinalDimensionConclusionResponse
    mapFinalDimension(
            AssessmentSessionResult session,
            FinalDimensionConclusion conclusion
    ) {
        String questionnairePreference =
                session
                        .initialResult()
                        .dimensions()
                        .stream()
                        .filter(initial ->
                                initial
                                        .dimension()
                                        .equals(
                                                conclusion.dimension()
                                        )
                        )
                        .map(initial ->
                                initial
                                        .questionnairePreference()
                                        == null
                                        ? null
                                        : initial
                                        .questionnairePreference()
                                        .value()
                        )
                        .findFirst()
                        .orElse(null);

        boolean overrodeBaseline =
                questionnairePreference != null
                        && !questionnairePreference
                        .equals(
                                conclusion
                                        .finalPreference()
                                        .value()
                        );

        return new AssessmentSessionResponse
                .FinalDimensionConclusionResponse(
                conclusion
                        .dimension()
                        .value(),
                questionnairePreference,
                conclusion
                        .finalPreference()
                        .value(),
                conclusion
                        .decisionSource()
                        .name(),
                overrodeBaseline
        );
    }

    public SessionQuestionnaireResponse
    toQuestionnaireResponse(
            GetSessionQuestionnaireResult result
    ) {
        var questions =
                result
                        .questions()
                        .stream()
                        .map(question ->
                                new SessionQuestionnaireResponse
                                        .PublicQuestionResponse(
                                        question.questionId(),
                                        question.position(),
                                        question.prompt()
                                )
                        )
                        .toList();

        var answerScale =
                result
                        .answerScale()
                        .stream()
                        .map(option ->
                                new SessionQuestionnaireResponse
                                        .AnswerScaleOptionResponse(
                                        option.value(),
                                        option.label()
                                )
                        )
                        .toList();

        var answers =
                result
                        .answers()
                        .stream()
                        .map(answer ->
                                new SessionQuestionnaireResponse
                                        .QuestionAnswerResponse(
                                        answer.questionId(),
                                        answer.value()
                                )
                        )
                        .toList();

        return new SessionQuestionnaireResponse(
                new SessionQuestionnaireResponse
                        .BoundAssessmentResponse(
                        result.assessmentCode(),
                        result.assessmentVersion()
                ),

                new SessionQuestionnaireResponse
                        .PublicQuestionnaireResponse(
                        questions,
                        answerScale
                ),

                new SessionQuestionnaireResponse
                        .QuestionnaireStateResponse(
                        answers,
                        result.submitted(),
                        result.submittedAt()
                )
        );
    }
}