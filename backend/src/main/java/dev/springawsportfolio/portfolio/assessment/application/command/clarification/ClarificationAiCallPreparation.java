package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;

import java.util.Objects;

public record ClarificationAiCallPreparation(
        Status status,
        ClarificationExecutionTicket ticket,
        ClarificationAiRequest request,
        int expectedLatestSequence
) {

    public enum Status {
        READY,
        STALE_DISCARDED,
        RUNTIME_STATE_LOST
    }

    public ClarificationAiCallPreparation {
        Objects.requireNonNull(
                status,
                "status must not be null"
        );

        Objects.requireNonNull(
                ticket,
                "ticket must not be null"
        );

        if (expectedLatestSequence < 0) {
            throw new IllegalArgumentException(
                    "expectedLatestSequence must not be negative"
            );
        }

        if (
                status == Status.READY
                        && request == null
        ) {
            throw new IllegalArgumentException(
                    "READY preparation must contain request"
            );
        }

        if (
                status != Status.READY
                        && request != null
        ) {
            throw new IllegalArgumentException(
                    "non-READY preparation must not contain request"
            );
        }
    }

    public static ClarificationAiCallPreparation ready(
            ClarificationExecutionTicket ticket,
            ClarificationAiRequest request,
            int expectedLatestSequence
    ) {
        return new ClarificationAiCallPreparation(
                Status.READY,
                ticket,
                request,
                expectedLatestSequence
        );
    }

    public static ClarificationAiCallPreparation stale(
            ClarificationExecutionTicket ticket
    ) {
        return new ClarificationAiCallPreparation(
                Status.STALE_DISCARDED,
                ticket,
                null,
                0
        );
    }

    public static ClarificationAiCallPreparation stateLost(
            ClarificationExecutionTicket ticket
    ) {
        return new ClarificationAiCallPreparation(
                Status.RUNTIME_STATE_LOST,
                ticket,
                null,
                0
        );
    }
}
