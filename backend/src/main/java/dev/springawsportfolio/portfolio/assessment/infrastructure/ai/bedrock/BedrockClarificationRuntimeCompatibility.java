package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimePolicy;

import java.time.Duration;
import java.util.Objects;

final class BedrockClarificationRuntimeCompatibility {

    static final Duration REQUIRED_LEASE_MARGIN =
            Duration.ofSeconds(
                    5
            );

    BedrockClarificationRuntimeCompatibility(
            BedrockClarificationProperties bedrockProperties,
            ClarificationRuntimePolicy runtimePolicy
    ) {
        Objects.requireNonNull(
                bedrockProperties,
                "bedrockProperties must not be null"
        );
        Objects.requireNonNull(
                runtimePolicy,
                "runtimePolicy must not be null"
        );

        Duration minimumLease =
                bedrockProperties
                        .apiCallTimeout()
                        .plus(
                                REQUIRED_LEASE_MARGIN
                        );

        if (
                runtimePolicy
                        .inFlightExecutionLease()
                        .compareTo(
                                minimumLease
                        ) <= 0
        ) {
            throw new IllegalArgumentException(
                    "inFlightExecutionLease must be greater than "
                            + "Bedrock apiCallTimeout plus safety margin"
            );
        }
    }
}
