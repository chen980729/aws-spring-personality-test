package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireResult;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.SessionQuestionnaireResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
public class AssessmentSessionQuestionnaireController {

    private final GetSessionQuestionnaireService
            getSessionQuestionnaireService;

    private final AssessmentSessionWebMapper
            sessionWebMapper;

    public AssessmentSessionQuestionnaireController(
            GetSessionQuestionnaireService getSessionQuestionnaireService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.getSessionQuestionnaireService =
                getSessionQuestionnaireService;

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
}