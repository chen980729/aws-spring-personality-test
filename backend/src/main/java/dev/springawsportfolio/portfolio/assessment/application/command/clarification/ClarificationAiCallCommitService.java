package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeContext;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimePolicy;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTurn;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.AIProvenance;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class ClarificationAiCallCommitService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final DimensionClarificationRepository
            clarificationRepository;

    private final ClarificationRuntimeContextStore
            runtimeContextStore;

    private final CompleteClarificationExecutionService
            completeExecutionService;

    private final ClarificationRuntimePolicy
            runtimePolicy;

    private final Clock clock;

    public ClarificationAiCallCommitService(
            AssessmentSessionRepository sessionRepository,
            DimensionClarificationRepository clarificationRepository,
            ClarificationRuntimeContextStore runtimeContextStore,
            CompleteClarificationExecutionService completeExecutionService,
            ClarificationRuntimePolicy runtimePolicy,
            Clock clock
    ) {
        this.sessionRepository =
                Objects.requireNonNull(
                        sessionRepository,
                        "sessionRepository must not be null"
                );

        this.clarificationRepository =
                Objects.requireNonNull(
                        clarificationRepository,
                        "clarificationRepository must not be null"
                );

        this.runtimeContextStore =
                Objects.requireNonNull(
                        runtimeContextStore,
                        "runtimeContextStore must not be null"
                );

        this.completeExecutionService =
                Objects.requireNonNull(
                        completeExecutionService,
                        "completeExecutionService must not be null"
                );

        this.runtimePolicy =
                Objects.requireNonNull(
                        runtimePolicy,
                        "runtimePolicy must not be null"
                );

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock must not be null"
                );
    }

    @Transactional
    public ClarificationRuntimeTurnResult commit(
            ClarificationAiCallPreparation preparation,
            ClarificationAiTurn turn
    ) {
        Objects.requireNonNull(
                preparation,
                "preparation must not be null"
        );

        Objects.requireNonNull(
                turn,
                "turn must not be null"
        );

        if (
                preparation.status()
                        != ClarificationAiCallPreparation.Status.READY
        ) {
            throw new IllegalArgumentException(
                    "only READY preparation can be committed"
            );
        }

        ClarificationExecutionTicket ticket =
                preparation.ticket();

        AssessmentSession session =
                sessionRepository
                        .findOwnedByIdForUpdate(
                                ticket.sessionId(),
                                ticket.actorUserId()
                        )
                        .orElse(null);

        if (
                session == null
                        || session.status()
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            return ClarificationRuntimeTurnResult.staleDiscarded();
        }

        DimensionClarification clarification =
                clarificationRepository
                        .findBySessionIdAndDimensionForUpdate(
                                ticket.sessionId(),
                                ticket.dimension()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "active clarification session is missing target clarification"
                                        )
                        );

        if (
                clarification.status()
                        != DimensionClarificationStatus.IN_PROGRESS
                        || !clarification.matchesActiveExecution(
                        ticket.executionToken()
                )
        ) {
            return ClarificationRuntimeTurnResult.staleDiscarded();
        }

        ClarificationRuntimeContext context =
                runtimeContextStore.loadActive(
                        ticket.sessionId(),
                        ticket.dimension(),
                        ticket.executionToken()
                );

        if (
                context.latestSequence()
                        != preparation.expectedLatestSequence()
        ) {
            return ClarificationRuntimeTurnResult.staleDiscarded();
        }

        Instant now =
                clock.instant();

        if (
                !context.isEmpty()
                        && !context
                        .messages()
                        .getFirst()
                        .expiresAt()
                        .isAfter(
                                now
                        )
        ) {
            return failActiveExecution(
                    ticket
            );
        }

        if (
                turn
                instanceof ClarificationAiTurn.FollowUpQuestion followUp
        ) {
            return commitFollowUp(
                    preparation,
                    followUp,
                    now
            );
        }

        if (
                turn
                instanceof ClarificationAiTurn.Judgement judgement
        ) {
            return commitJudgement(
                    preparation,
                    judgement
            );
        }

        throw new IllegalStateException(
                "unsupported ClarificationAiTurn implementation"
        );
    }

    @Transactional
    public ClarificationRuntimeTurnResult failRetryable(
            ClarificationExecutionTicket ticket
    ) {
        Objects.requireNonNull(
                ticket,
                "ticket must not be null"
        );

        return failActiveExecution(
                ticket
        );
    }

    private ClarificationRuntimeTurnResult commitFollowUp(
            ClarificationAiCallPreparation preparation,
            ClarificationAiTurn.FollowUpQuestion followUp,
            Instant now
    ) {
        int nextSequence =
                preparation.expectedLatestSequence() + 1;

        ClarificationRuntimeMessage message =
                new ClarificationRuntimeMessage(
                        nextSequence,
                        ClarificationRuntimeMessage.Role.ASSISTANT_QUESTION,
                        followUp.text(),
                        now,
                        now.plus(
                                runtimePolicy.contextTtl()
                        )
                );

        boolean appended =
                runtimeContextStore
                        .appendIfLatestSequenceMatches(
                                preparation.ticket().sessionId(),
                                preparation.ticket().dimension(),
                                preparation.ticket().executionToken(),
                                preparation.expectedLatestSequence(),
                                message
                        );

        if (!appended) {
            return ClarificationRuntimeTurnResult.staleDiscarded();
        }

        return ClarificationRuntimeTurnResult.followUp(
                nextSequence,
                followUp.text()
        );
    }

    private ClarificationRuntimeTurnResult commitJudgement(
            ClarificationAiCallPreparation preparation,
            ClarificationAiTurn.Judgement judgement
    ) {
        ClarificationResult result =
                new ClarificationResult(
                        judgement.resolution(),
                        judgement.suggestedPole(),
                        judgement.confidence(),
                        judgement.reasoningSummary()
                );

        AIProvenance provenance =
                new AIProvenance(
                        judgement
                                .modelIdentity()
                                .provider(),
                        judgement
                                .modelIdentity()
                                .modelIdentifier(),
                        preparation
                                .request()
                                .clarificationPolicyRevision()
                );

        ClarificationExecutionCompletionResult completion =
                completeExecutionService.accept(
                        preparation.ticket(),
                        result,
                        provenance
                );

        if (!completion.accepted()) {
            return ClarificationRuntimeTurnResult.staleDiscarded();
        }

        runtimeContextStore.deleteDimension(
                preparation.ticket().sessionId(),
                preparation.ticket().dimension()
        );

        return ClarificationRuntimeTurnResult.judgementAccepted(
                completion.session()
        );
    }

    private ClarificationRuntimeTurnResult failActiveExecution(
            ClarificationExecutionTicket ticket
    ) {
        ClarificationExecutionCompletionResult completion =
                completeExecutionService.failRetryable(
                        ticket
                );

        if (!completion.accepted()) {
            return ClarificationRuntimeTurnResult.staleDiscarded();
        }

        runtimeContextStore.deleteDimension(
                ticket.sessionId(),
                ticket.dimension()
        );

        return ClarificationRuntimeTurnResult.failedRetryable(
                completion.session()
        );
    }
}
