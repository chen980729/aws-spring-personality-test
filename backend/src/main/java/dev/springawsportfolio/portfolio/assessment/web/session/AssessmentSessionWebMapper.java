package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.start.StartAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireResult;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
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
        /*
         * Before Submit is implemented, the current application
         * can truthfully construct the complete Session HTTP model
         * only for a pre-submission IN_PROGRESS Session.
         *
         * Do not silently fabricate post-submission result data.
         */
        if (
                session.status()
                        != AssessmentSessionStatus.IN_PROGRESS
        ) {
            throw new IllegalStateException(
                    "full HTTP session mapping "
                            + "is not yet implemented for status: "
                            + session.status()
            );
        }

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

                null,

                List.of(),

                List.of(),

                null,

                new AssessmentSessionResponse
                        .AssessmentWorkflowResponse(
                        List.of(),
                        List.of(),
                        List.of(),
                        false
                ),

                session.createdAt(),
                session.completedAt(),
                session.abandonedAt()
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