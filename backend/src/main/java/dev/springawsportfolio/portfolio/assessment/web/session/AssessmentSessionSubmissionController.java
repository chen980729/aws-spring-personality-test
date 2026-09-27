package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireResult;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.QuestionnaireSnapshotRequest;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
public class AssessmentSessionSubmissionController {

    private final SubmitQuestionnaireService
            submitQuestionnaireService;

    private final AssessmentSessionWebMapper
            sessionWebMapper;

    public AssessmentSessionSubmissionController(
            SubmitQuestionnaireService submitQuestionnaireService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.submitQuestionnaireService =
                submitQuestionnaireService;

        this.sessionWebMapper =
                sessionWebMapper;
    }

    @PostMapping("/{sessionId}/questionnaire/submission")
    public AssessmentSessionResponse submitQuestionnaire(
            @PathVariable
            UUID sessionId,

            @Valid
            @RequestBody
            QuestionnaireSnapshotRequest request,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        List<SubmitQuestionnaireCommand.AnswerInput>
                answers =
                request
                        .answers()
                        .stream()
                        .map(answer ->
                                new SubmitQuestionnaireCommand
                                        .AnswerInput(
                                        answer.questionId(),
                                        answer.value()
                                )
                        )
                        .toList();

        SubmitQuestionnaireResult result =
                submitQuestionnaireService.execute(
                        new SubmitQuestionnaireCommand(
                                new UserId(
                                        principal.userId()
                                ),
                                new AssessmentSessionId(
                                        sessionId
                                ),
                                answers
                        )
                );

        return sessionWebMapper
                .toSessionResponse(
                        result.session()
                );
    }
}
