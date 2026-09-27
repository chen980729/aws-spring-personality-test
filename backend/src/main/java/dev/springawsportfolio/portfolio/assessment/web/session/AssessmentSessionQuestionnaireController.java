package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.questionnaire.SaveQuestionnaireProgressCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.questionnaire.SaveQuestionnaireProgressService;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireResult;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.QuestionnaireSnapshotRequest;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.SessionQuestionnaireResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
public class AssessmentSessionQuestionnaireController {

    private final GetSessionQuestionnaireService
            getSessionQuestionnaireService;

    private final SaveQuestionnaireProgressService
            saveQuestionnaireProgressService;

    private final AssessmentSessionWebMapper
            sessionWebMapper;

    public AssessmentSessionQuestionnaireController(
            GetSessionQuestionnaireService getSessionQuestionnaireService,
            SaveQuestionnaireProgressService saveQuestionnaireProgressService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.getSessionQuestionnaireService =
                getSessionQuestionnaireService;

        this.saveQuestionnaireProgressService =
                saveQuestionnaireProgressService;

        this.sessionWebMapper =
                sessionWebMapper;
    }

    @GetMapping("/{sessionId}/questionnaire")
    public SessionQuestionnaireResponse
    getSessionQuestionnaire(
            @PathVariable
            UUID sessionId,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        GetSessionQuestionnaireResult result =
                getSessionQuestionnaireService.execute(
                        new UserId(
                                principal.userId()
                        ),
                        new AssessmentSessionId(
                                sessionId
                        )
                );

        return sessionWebMapper
                .toQuestionnaireResponse(
                        result
                );
    }

    @PutMapping("/{sessionId}/questionnaire")
    public AssessmentSessionResponse
    saveQuestionnaireProgress(
            @PathVariable
            UUID sessionId,

            @Valid
            @RequestBody
            QuestionnaireSnapshotRequest request,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        List<SaveQuestionnaireProgressCommand.AnswerInput>
                answers =
                request
                        .answers()
                        .stream()
                        .map(answer ->
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        answer.questionId(),
                                        answer.value()
                                )
                        )
                        .toList();

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        new UserId(
                                principal.userId()
                        ),
                        new AssessmentSessionId(
                                sessionId
                        ),
                        answers
                );

        AssessmentSessionResult result =
                saveQuestionnaireProgressService.execute(
                        command
                );

        return sessionWebMapper.toSessionResponse(
                result
        );
    }
}