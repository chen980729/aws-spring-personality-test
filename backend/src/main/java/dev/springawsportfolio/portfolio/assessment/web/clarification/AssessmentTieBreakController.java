package dev.springawsportfolio.portfolio.assessment.web.clarification;

import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.web.clarification.dto.TieBreakRequest;
import dev.springawsportfolio.portfolio.assessment.web.session.AssessmentSessionWebMapper;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
public class AssessmentTieBreakController {

    private final SubmitDimensionTieBreakService
            submitDimensionTieBreakService;

    private final AssessmentSessionWebMapper
            sessionWebMapper;

    public AssessmentTieBreakController(
            SubmitDimensionTieBreakService submitDimensionTieBreakService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.submitDimensionTieBreakService =
                submitDimensionTieBreakService;

        this.sessionWebMapper =
                sessionWebMapper;
    }

    @PutMapping(
            "/{sessionId}/tie-breaks/{dimensionCode}"
    )
    public AssessmentSessionResponse submitTieBreak(
            @PathVariable
            UUID sessionId,

            @PathVariable
            String dimensionCode,

            @Valid
            @RequestBody
            TieBreakRequest request,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        AssessmentSessionResult result =
                submitDimensionTieBreakService.execute(
                        new SubmitDimensionTieBreakCommand(
                                new UserId(
                                        principal.userId()
                                ),
                                new AssessmentSessionId(
                                        sessionId
                                ),
                                new DimensionCode(
                                        dimensionCode
                                ),
                                new PoleCode(
                                        request.selectedPole()
                                )
                        )
                );

        return sessionWebMapper.toSessionResponse(
                result
        );
    }
}
