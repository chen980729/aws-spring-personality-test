package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.start.StartAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.application.command.start.StartAssessmentService;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetActiveAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.StartAssessmentResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentSessionController {

    private final StartAssessmentService
            startAssessmentService;

    private final GetActiveAssessmentSessionService
            getActiveAssessmentSessionService;

    private final AssessmentSessionWebMapper
            sessionWebMapper;

    public AssessmentSessionController(
            StartAssessmentService startAssessmentService,
            GetActiveAssessmentSessionService getActiveAssessmentSessionService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.startAssessmentService =
                startAssessmentService;

        this.getActiveAssessmentSessionService =
                getActiveAssessmentSessionService;

        this.sessionWebMapper =
                sessionWebMapper;
    }

    @PostMapping("/{assessmentCode}/sessions")
    public ResponseEntity<StartAssessmentResponse>
    startAssessment(
            @PathVariable
            String assessmentCode,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        StartAssessmentResult result =
                startAssessmentService.execute(
                        new UserId(
                                principal.userId()
                        ),
                        assessmentCode
                );

        StartAssessmentResponse response =
                sessionWebMapper.toStartResponse(
                        result
                );

        if (result.created()) {
            URI location =
                    URI.create(
                            "/api/v1/assessment-sessions/"
                                    + result.session().id()
                    );

            return ResponseEntity
                    .created(location)
                    .body(response);
        }

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/{assessmentCode}/sessions/active")
    public AssessmentSessionResponse
    getActiveAssessmentSession(
            @PathVariable
            String assessmentCode,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        AssessmentSessionResult result =
                getActiveAssessmentSessionService.execute(
                        new UserId(
                                principal.userId()
                        ),
                        assessmentCode
                );

        return sessionWebMapper.toSessionResponse(
                result
        );
    }
}