package dev.springawsportfolio.portfolio.assessment.domain.clarification;

import java.util.Objects;
import java.util.UUID;

public record ClarificationExecutionToken(
        UUID value
) {

    public ClarificationExecutionToken {
        Objects.requireNonNull(
                value,
                "value must not be null"
        );
    }

    public static ClarificationExecutionToken newToken() {
        return new ClarificationExecutionToken(
                UUID.randomUUID()
        );
    }
}
