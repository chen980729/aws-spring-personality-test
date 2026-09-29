package dev.springawsportfolio.portfolio.assessment.web.clarification;

import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipRemainingClarificationsCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipRemainingClarificationsService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.web.session.AssessmentSessionWebMapper;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
public class AssessmentClarificationController {

    private final SkipDimensionClarificationService
            skipDimensionClarificationService;

    private final SkipRemainingClarificationsService
            skipRemainingClarificationsService;

    private final AssessmentSessionWebMapper
            sessionWebMapper;

    public AssessmentClarificationController(
            SkipDimensionClarificationService skipDimensionClarificationService,
            SkipRemainingClarificationsService skipRemainingClarificationsService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.skipDimensionClarificationService =
                skipDimensionClarificationService;

        this.skipRemainingClarificationsService =
                skipRemainingClarificationsService;

        this.sessionWebMapper =
                sessionWebMapper;
    }

    @PostMapping(
            "/{sessionId}/clarifications/{dimensionCode}/skip"
    )
    public AssessmentSessionResponse skipClarification(
            @PathVariable
            UUID sessionId,

            @PathVariable
            String dimensionCode,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        AssessmentSessionResult result =
                skipDimensionClarificationService.execute(
                        new SkipDimensionClarificationCommand(
                                new UserId(
                                        principal.userId()
                                ),
                                new AssessmentSessionId(
                                        sessionId
                                ),
                                new DimensionCode(
                                        dimensionCode
                                )
                        )
                );

        return sessionWebMapper.toSessionResponse(
                result
        );
    }

    @PostMapping(
            "/{sessionId}/clarifications/skip-remaining"
    )
    public AssessmentSessionResponse skipRemainingClarifications(
            @PathVariable
            UUID sessionId,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        AssessmentSessionResult result =
                skipRemainingClarificationsService.execute(
                        new SkipRemainingClarificationsCommand(
                                new UserId(
                                        principal.userId()
                                ),
                                new AssessmentSessionId(
                                        sessionId
                                )
                        )
                );

        return sessionWebMapper.toSessionResponse(
                result
        );
    }
}
