package dev.springawsportfolio.portfolio.assessment.web.clarification;

import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationRuntimeOrchestrator;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationRuntimeTurnResult;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ContinueDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.RetryDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationTurnConflictException;
import dev.springawsportfolio.portfolio.assessment.application.query.clarification.CurrentClarificationTurnResult;
import dev.springawsportfolio.portfolio.assessment.application.query.clarification.GetCurrentClarificationTurnService;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.web.clarification.dto.ClarificationInteractionResponse;
import dev.springawsportfolio.portfolio.assessment.web.clarification.dto.ClarificationMessageRequest;
import dev.springawsportfolio.portfolio.assessment.web.session.AssessmentSessionWebMapper;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-sessions/{sessionId}/clarifications/{dimensionCode}")
public class ClarificationRuntimeController {

    private final ObjectProvider<ClarificationRuntimeOrchestrator> orchestratorProvider;
    private final GetCurrentClarificationTurnService currentTurnService;
    private final GetAssessmentSessionService getSessionService;
    private final AssessmentSessionWebMapper sessionWebMapper;

    public ClarificationRuntimeController(
            ObjectProvider<ClarificationRuntimeOrchestrator> orchestratorProvider,
            GetCurrentClarificationTurnService currentTurnService,
            GetAssessmentSessionService getSessionService,
            AssessmentSessionWebMapper sessionWebMapper
    ) {
        this.orchestratorProvider = orchestratorProvider;
        this.currentTurnService = currentTurnService;
        this.getSessionService = getSessionService;
        this.sessionWebMapper = sessionWebMapper;
    }

    @PostMapping("/start")
    public ClarificationInteractionResponse start(
            @PathVariable UUID sessionId,
            @PathVariable String dimensionCode,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        UserId actor = new UserId(principal.userId());
        AssessmentSessionId session = new AssessmentSessionId(sessionId);
        DimensionCode dimension = new DimensionCode(dimensionCode);

        return toResponse(
                actor,
                session,
                dimension,
                requireOrchestrator().start(
                        new StartDimensionClarificationCommand(actor, session, dimension)
                )
        );
    }

    /**
     * Refresh/lost-response recovery: this read works even when the
     * provider-backed commands are disabled and never invokes the model.
     */
    @GetMapping("/current-turn")
    public CurrentClarificationTurnResult currentTurn(
            @PathVariable UUID sessionId,
            @PathVariable String dimensionCode,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return currentTurnService.execute(
                new UserId(principal.userId()),
                new AssessmentSessionId(sessionId),
                new DimensionCode(dimensionCode)
        );
    }

    @PostMapping("/messages")
    public ClarificationInteractionResponse continueWithAnswer(
            @PathVariable UUID sessionId,
            @PathVariable String dimensionCode,
            @Valid @RequestBody ClarificationMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        UserId actor = new UserId(principal.userId());
        AssessmentSessionId session = new AssessmentSessionId(sessionId);
        DimensionCode dimension = new DimensionCode(dimensionCode);

        return toResponse(
                actor,
                session,
                dimension,
                requireOrchestrator().continueClarification(
                        new ContinueDimensionClarificationCommand(
                                actor, session, dimension,
                                request.assistantSequence(), request.answer()
                        )
                )
        );
    }

    @PostMapping("/retry")
    public ClarificationInteractionResponse retry(
            @PathVariable UUID sessionId,
            @PathVariable String dimensionCode,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        UserId actor = new UserId(principal.userId());
        AssessmentSessionId session = new AssessmentSessionId(sessionId);
        DimensionCode dimension = new DimensionCode(dimensionCode);

        return toResponse(
                actor,
                session,
                dimension,
                requireOrchestrator().retry(
                        new RetryDimensionClarificationCommand(actor, session, dimension)
                )
        );
    }

    private ClarificationRuntimeOrchestrator requireOrchestrator() {
        ClarificationRuntimeOrchestrator orchestrator = orchestratorProvider.getIfAvailable();
        if (orchestrator == null) {
            throw new AiClarificationUnavailableException();
        }
        return orchestrator;
    }

    private ClarificationInteractionResponse toResponse(
            UserId actor,
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            ClarificationRuntimeTurnResult turn
    ) {
        if (turn.status() == ClarificationRuntimeTurnResult.Status.STALE_DISCARDED) {
            throw new ClarificationTurnConflictException(
                    "clarification state changed during model execution"
            );
        }

        AssessmentSessionResult session = turn.session() == null
                ? getSessionService.execute(actor, sessionId)
                : turn.session();

        AssessmentSessionResponse sessionResponse = sessionWebMapper.toSessionResponse(session);
        AssessmentSessionResponse.ClarificationStateResponse clarification =
                sessionResponse.clarifications().stream()
                        .filter(candidate -> candidate.dimensionCode().equals(dimension.value()))
                        .findFirst()
                        .orElseThrow(() -> new ClarificationTurnConflictException(
                                "clarification state changed during model execution"
                        ));

        if (turn.status() == ClarificationRuntimeTurnResult.Status.FOLLOW_UP_REQUIRED
                && !"IN_PROGRESS".equals(clarification.status())) {
            throw new ClarificationTurnConflictException(
                    "assistant question is no longer active"
            );
        }

        return new ClarificationInteractionResponse(
                sessionId.value(),
                dimension.value(),
                clarification.status(),
                turn.assistantSequence(),
                turn.followUpQuestion(),
                clarification.result(),
                sessionResponse.status(),
                sessionResponse.workflow()
        );
    }
}
