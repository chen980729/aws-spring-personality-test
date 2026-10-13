package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiPort;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTechnicalException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTurn;

import java.util.Objects;

public final class ClarificationRuntimeOrchestrator {

    private final StartDimensionClarificationService
            startService;

    private final RetryDimensionClarificationService
            retryService;

    private final ClarificationAiCallPreparationService
            preparationService;

    private final ClarificationAiCallCommitService
            commitService;

    private final ClarificationExecutionRecoveryService
            recoveryService;

    private final ClarificationAiPort
            aiPort;

    public ClarificationRuntimeOrchestrator(
            StartDimensionClarificationService startService,
            RetryDimensionClarificationService retryService,
            ClarificationAiCallPreparationService preparationService,
            ClarificationAiCallCommitService commitService,
            ClarificationExecutionRecoveryService recoveryService,
            ClarificationAiPort aiPort
    ) {
        this.startService =
                Objects.requireNonNull(
                        startService,
                        "startService must not be null"
                );

        this.retryService =
                Objects.requireNonNull(
                        retryService,
                        "retryService must not be null"
                );

        this.preparationService =
                Objects.requireNonNull(
                        preparationService,
                        "preparationService must not be null"
                );

        this.commitService =
                Objects.requireNonNull(
                        commitService,
                        "commitService must not be null"
                );

        this.recoveryService =
                Objects.requireNonNull(
                        recoveryService,
                        "recoveryService must not be null"
                );

        this.aiPort =
                Objects.requireNonNull(
                        aiPort,
                        "aiPort must not be null"
                );
    }

    public ClarificationRuntimeTurnResult start(
            StartDimensionClarificationCommand command
    ) {
        ClarificationExecutionTicket ticket =
                startService.execute(
                        Objects.requireNonNull(
                                command,
                                "command must not be null"
                        )
                );

        return executePrepared(
                preparationService.prepareInitial(
                        ticket
                )
        );
    }

    public ClarificationRuntimeTurnResult retry(
            RetryDimensionClarificationCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        recoveryService.recoverExpiredExecutionForRetry(
                command
        );

        ClarificationExecutionTicket ticket =
                retryService.execute(
                        command
                );

        return executePrepared(
                preparationService.prepareInitial(
                        ticket
                )
        );
    }

    public ClarificationRuntimeTurnResult continueClarification(
            ContinueDimensionClarificationCommand command
    ) {
        return executePrepared(
                preparationService.prepareContinue(
                        Objects.requireNonNull(
                                command,
                                "command must not be null"
                        )
                )
        );
    }

    private ClarificationRuntimeTurnResult executePrepared(
            ClarificationAiCallPreparation preparation
    ) {
        return switch (preparation.status()) {
            case STALE_DISCARDED ->
                    ClarificationRuntimeTurnResult.staleDiscarded();

            case RUNTIME_STATE_LOST ->
                    commitService.failRetryable(
                            preparation.ticket()
                    );

            case READY ->
                    executeExternalCall(
                            preparation
                    );
        };
    }

    private ClarificationRuntimeTurnResult executeExternalCall(
            ClarificationAiCallPreparation preparation
    ) {
        ClarificationAiTurn turn;

        try {
            turn =
                    aiPort.nextTurn(
                            preparation.request()
                    );
        } catch (
                ClarificationAiTechnicalException technicalFailure
        ) {
            return commitService.failRetryable(
                    preparation.ticket()
            );
        } catch (RuntimeException unexpectedFailure) {
            try {
                commitService.failRetryable(
                        preparation.ticket()
                );
            } catch (RuntimeException compensationFailure) {
                unexpectedFailure.addSuppressed(
                        compensationFailure
                );
            }

            throw unexpectedFailure;
        }

        return commitService.commit(
                preparation,
                turn
        );
    }
}
