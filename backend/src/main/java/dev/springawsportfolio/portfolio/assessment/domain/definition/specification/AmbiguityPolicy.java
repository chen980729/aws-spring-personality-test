package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record AmbiguityPolicy(
        AmbiguityPolicyType type,
        String revision,
        int inclusiveThreshold
) {
    public AmbiguityPolicy {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(revision, "revision must not be null");

        if (revision.isBlank()) {
            throw new IllegalArgumentException(
                    "revision must not be blank"
            );
        }

        if (inclusiveThreshold < 0) {
            throw new IllegalArgumentException(
                    "inclusiveThreshold must not be negative"
            );
        }
    }
}
