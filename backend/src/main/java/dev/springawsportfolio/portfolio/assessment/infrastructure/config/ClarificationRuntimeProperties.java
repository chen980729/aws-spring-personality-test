package dev.springawsportfolio.portfolio.assessment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(
        prefix = "app.assessment.clarification.runtime"
)
public record ClarificationRuntimeProperties(
        Integer maximumModelTurns,
        Duration contextTtl
) {

    private static final int DEFAULT_MAXIMUM_MODEL_TURNS =
            3;

    private static final Duration DEFAULT_CONTEXT_TTL =
            Duration.ofHours(
                    24
            );

    public ClarificationRuntimeProperties {
        maximumModelTurns =
                maximumModelTurns == null
                        ? DEFAULT_MAXIMUM_MODEL_TURNS
                        : maximumModelTurns;

        contextTtl =
                contextTtl == null
                        ? DEFAULT_CONTEXT_TTL
                        : contextTtl;

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
    }
}
