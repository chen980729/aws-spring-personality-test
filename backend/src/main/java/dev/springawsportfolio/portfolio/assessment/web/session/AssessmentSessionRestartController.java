package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.RestartAssessmentResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
public class AssessmentSessionRestartController {

    private final RestartAssessmentSessionService
            restartAssessmentSessionService;

    private final AssessmentSessionWebMapper
            sessionWebMapper;

    public AssessmentSessionRestartController(
            RestartAssessmentSessionService restartAssessmentSessionService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.restartAssessmentSessionService =
                restartAssessmentSessionService;

        this.sessionWebMapper =
                sessionWebMapper;
    }

    @PostMapping("/{sessionId}/restart")
    public ResponseEntity<RestartAssessmentResponse>
    restartAssessmentSession(
            @PathVariable
            UUID sessionId,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        RestartAssessmentSessionResult result =
                restartAssessmentSessionService.execute(
                        new RestartAssessmentSessionCommand(
                                new UserId(
                                        principal.userId()
                                ),
                                new AssessmentSessionId(
                                        sessionId
                                )
                        )
                );

        RestartAssessmentResponse response =
                new RestartAssessmentResponse(
                        result.abandonedSessionId(),
                        sessionWebMapper.toSessionResponse(
                                result.session()
                        )
                );

        URI location =
                URI.create(
                        "/api/v1/assessment-sessions/"
                                + result
                                .session()
                                .id()
                );

        return ResponseEntity
                .created(
                        location
                )
                .body(
                        response
                );
    }
}
