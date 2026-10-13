package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeContext;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimePolicy;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationTurnConflictException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
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
public class ClarificationExecutionRecoveryService {

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

    public ClarificationExecutionRecoveryService(
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
    public void recoverExpiredExecutionForRetry(
            RetryDimensionClarificationCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        AssessmentSession session =
                sessionRepository
                        .findOwnedByIdForUpdate(
                                command.sessionId(),
                                command.actorUserId()
                        )
                        .orElseThrow(
                                AssessmentSessionNotFoundException::new
                        );

        if (
                session.status()
                        == AssessmentSessionStatus.AWAITING_CLARIFICATION
        ) {
            return;
        }

        if (
                session.status()
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            return;
        }

        DimensionClarification clarification =
                clarificationRepository
                        .findBySessionIdAndDimensionForUpdate(
                                command.sessionId(),
                                command.dimension()
                        )
                        .orElseThrow(
                                () ->
                                        new ClarificationNotAllowedException(
                                                "clarification does not exist for target dimension"
                                        )
                        );

        if (
                clarification.status()
                        != DimensionClarificationStatus.IN_PROGRESS
                        || clarification.activeExecutionToken() == null
        ) {
            throw new ClarificationNotAllowedException(
                    "target clarification does not own the active execution"
            );
        }

        ClarificationRuntimeContext context =
                runtimeContextStore.loadActive(
                        command.sessionId(),
                        command.dimension(),
                        clarification.activeExecutionToken()
                );

        Instant now =
                clock.instant();

        if (
                !isRecoverable(
                        clarification,
                        context,
                        now
                )
        ) {
            throw new ClarificationTurnConflictException(
                    "clarification execution is still active or awaiting an answer"
            );
        }

        ClarificationExecutionTicket ticket =
                new ClarificationExecutionTicket(
                        command.actorUserId(),
                        command.sessionId(),
                        command.dimension(),
                        clarification.activeExecutionToken()
                );

        ClarificationExecutionCompletionResult completion =
                completeExecutionService.failRetryable(
                        ticket
                );

        if (!completion.accepted()) {
            throw new IllegalStateException(
                    "expired clarification execution became stale during recovery"
            );
        }

        runtimeContextStore.deleteDimension(
                ticket.sessionId(),
                ticket.dimension()
        );
    }

    private boolean isRecoverable(
            DimensionClarification clarification,
            ClarificationRuntimeContext context,
            Instant now
    ) {
        if (context.isEmpty()) {
            return leaseExpired(
                    clarification.updatedAt(),
                    now
            );
        }

        if (
                !context
                        .messages()
                        .getFirst()
                        .expiresAt()
                        .isAfter(
                                now
                        )
        ) {
            return true;
        }

        ClarificationRuntimeMessage latest =
                context
                        .messages()
                        .getLast();

        if (
                latest.role()
                        == ClarificationRuntimeMessage.Role.USER_ANSWER
        ) {
            return leaseExpired(
                    latest.createdAt(),
                    now
            );
        }

        return false;
    }

    private boolean leaseExpired(
            Instant beganAt,
            Instant now
    ) {
        return !now.isBefore(
                beganAt.plus(
                        runtimePolicy.inFlightExecutionLease()
                )
        );
    }
}
