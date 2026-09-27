package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.query.session.GetAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
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
public class AssessmentSessionDetailController {

    private final GetAssessmentSessionService
            getAssessmentSessionService;

    private final AssessmentSessionWebMapper
            sessionWebMapper;

    public AssessmentSessionDetailController(
            GetAssessmentSessionService getAssessmentSessionService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.getAssessmentSessionService =
                getAssessmentSessionService;

        this.sessionWebMapper =
                sessionWebMapper;
    }

    @GetMapping("/{sessionId}")
    public AssessmentSessionResponse
    getAssessmentSession(
            @PathVariable
            UUID sessionId,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        AssessmentSessionResult result =
                getAssessmentSessionService.execute(
                        new UserId(
                                principal.userId()
                        ),
                        new AssessmentSessionId(
                                sessionId
                        )
                );

        return sessionWebMapper
                .toSessionResponse(
                        result
                );
    }
}
