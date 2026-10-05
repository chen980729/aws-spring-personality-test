package dev.springawsportfolio.portfolio.assessment.web.clarification;

import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakService;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.TieBreakSelection;
import dev.springawsportfolio.portfolio.assessment.application.query.tiebreak.DimensionTieBreakInteractionResult;
import dev.springawsportfolio.portfolio.assessment.application.query.tiebreak.GetDimensionTieBreakInteractionService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakOptionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakQuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.web.clarification.dto.TieBreakInteractionResponse;
import dev.springawsportfolio.portfolio.assessment.web.clarification.dto.TieBreakRequest;
import dev.springawsportfolio.portfolio.assessment.web.session.AssessmentSessionWebMapper;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
public class AssessmentTieBreakController {

    private final GetDimensionTieBreakInteractionService
            getDimensionTieBreakInteractionService;

    private final SubmitDimensionTieBreakService
            submitDimensionTieBreakService;

    private final AssessmentSessionWebMapper
            sessionWebMapper;

    public AssessmentTieBreakController(
            GetDimensionTieBreakInteractionService getDimensionTieBreakInteractionService,
            SubmitDimensionTieBreakService submitDimensionTieBreakService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.getDimensionTieBreakInteractionService =
                getDimensionTieBreakInteractionService;

        this.submitDimensionTieBreakService =
                submitDimensionTieBreakService;

        this.sessionWebMapper =
                sessionWebMapper;
    }

    @GetMapping(
            "/{sessionId}/tie-breaks/{dimensionCode}"
    )
    public TieBreakInteractionResponse getTieBreakInteraction(
            @PathVariable
            UUID sessionId,

            @PathVariable
            String dimensionCode,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        DimensionTieBreakInteractionResult result =
                getDimensionTieBreakInteractionService.execute(
                        new UserId(
                                principal.userId()
                        ),
                        new AssessmentSessionId(
                                sessionId
                        ),
                        new DimensionCode(
                                dimensionCode
                        )
                );

        return toInteractionResponse(
                result
        );
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
                                toSelection(
                                        request
                                )
                        )
                );

        return sessionWebMapper.toSessionResponse(
                result
        );
    }

    private TieBreakSelection toSelection(
            TieBreakRequest request
    ) {
        if (request.isLegacyShape()) {
            return new TieBreakSelection.DirectPoleSelection(
                    new PoleCode(
                            request.selectedPole()
                    )
            );
        }

        if (request.isContextualShape()) {
            return new TieBreakSelection.ContextualOptionSelection(
                    new TieBreakQuestionId(
                            request.questionId()
                    ),
                    new TieBreakOptionId(
                            request.selectedOptionId()
                    )
            );
        }

        throw new IllegalStateException(
                "validated tie-break request has no recognized shape"
        );
    }

    private TieBreakInteractionResponse toInteractionResponse(
            DimensionTieBreakInteractionResult result
    ) {
        if (
                result
                        instanceof DimensionTieBreakInteractionResult
                        .DirectPoleSelection direct
        ) {
            return new TieBreakInteractionResponse
                    .DirectPoleSelection(
                    "DIRECT_POLE_SELECTION",
                    direct.dimensionCode(),
                    direct.allowedPoles()
            );
        }

        DimensionTieBreakInteractionResult.ContextualQuestion
                contextual =
                (DimensionTieBreakInteractionResult.ContextualQuestion)
                        result;

        return new TieBreakInteractionResponse.ContextualQuestion(
                "CONTEXTUAL_QUESTION",
                contextual.dimensionCode(),
                contextual.questionId(),
                contextual.instruction(),
                contextual.prompt(),
                contextual
                        .options()
                        .stream()
                        .map(option ->
                                new TieBreakInteractionResponse.Option(
                                        option.optionId(),
                                        option.text()
                                )
                        )
                        .toList()
        );
    }
}
