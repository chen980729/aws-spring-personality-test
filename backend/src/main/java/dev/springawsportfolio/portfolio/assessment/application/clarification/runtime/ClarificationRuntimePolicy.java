package dev.springawsportfolio.portfolio.assessment.application.clarification.runtime;

import java.time.Duration;
import java.util.Objects;

public record ClarificationRuntimePolicy(
        int maximumModelTurns,
        Duration contextTtl,
        Duration inFlightExecutionLease
) {

    public ClarificationRuntimePolicy {
        if (maximumModelTurns <= 0) {
            throw new IllegalArgumentException(
                    "maximumModelTurns must be positive"
            );
        }

        Objects.requireNonNull(
                contextTtl,
                "contextTtl must not be null"
        );

        if (
                contextTtl.isZero()
                        || contextTtl.isNegative()
        ) {
            throw new IllegalArgumentException(
                    "contextTtl must be positive"
            );
        }

        Objects.requireNonNull(
                inFlightExecutionLease,
                "inFlightExecutionLease must not be null"
        );

        if (
                inFlightExecutionLease.isZero()
                        || inFlightExecutionLease.isNegative()
        ) {
            throw new IllegalArgumentException(
                    "inFlightExecutionLease must be positive"
            );
        }

        if (
                inFlightExecutionLease.compareTo(
                        contextTtl
                ) >= 0
        ) {
            throw new IllegalArgumentException(
                    "inFlightExecutionLease must be shorter than contextTtl"
            );
        }
    }
}
