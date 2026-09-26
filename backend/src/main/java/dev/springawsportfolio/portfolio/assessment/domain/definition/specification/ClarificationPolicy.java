package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record ClarificationPolicy(
        ClarificationPolicyType type,
        String revision
) {
    public ClarificationPolicy {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(revision, "revision must not be null");

        if (revision.isBlank()) {
            throw new IllegalArgumentException(
                    "revision must not be blank"
            );
        }
    }
}
