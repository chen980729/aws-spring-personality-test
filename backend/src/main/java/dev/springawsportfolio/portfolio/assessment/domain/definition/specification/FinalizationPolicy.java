package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;
import java.util.Set;

public record FinalizationPolicy(
        FinalizationPolicyType type,
        String revision
) {

    public static final String LEGACY_DIRECT_TIE_BREAK_REVISION =
            "v1";

    public static final String CONTEXTUAL_TIE_BREAK_REVISION =
            "v2";

    private static final Set<String> SUPPORTED_REVISIONS =
            Set.of(
                    LEGACY_DIRECT_TIE_BREAK_REVISION,
                    CONTEXTUAL_TIE_BREAK_REVISION
            );

    public FinalizationPolicy {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(revision, "revision must not be null");

        if (revision.isBlank()) {
            throw new IllegalArgumentException(
                    "revision must not be blank"
            );
        }

        if (!SUPPORTED_REVISIONS.contains(revision)) {
            throw new IllegalArgumentException(
                    "unsupported finalization policy revision: "
                            + revision
            );
        }
    }

    public boolean usesLegacyDirectTieBreak() {
        return revision.equals(
                LEGACY_DIRECT_TIE_BREAK_REVISION
        );
    }

    public boolean usesContextualTieBreakQuestions() {
        return revision.equals(
                CONTEXTUAL_TIE_BREAK_REVISION
        );
    }
}
