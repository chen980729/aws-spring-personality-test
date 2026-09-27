package dev.springawsportfolio.portfolio.assessment.domain.clarification;

import java.util.Objects;
import java.util.UUID;

public record DimensionClarificationId(
        UUID value
) {

    public DimensionClarificationId {
        Objects.requireNonNull(
                value,
                "value must not be null"
        );
    }

    public static DimensionClarificationId newId() {
        return new DimensionClarificationId(
                UUID.randomUUID()
        );
    }
}
