package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalizationPolicyTest {

    @Test
    void identifiesLegacyAndContextualRevisions() {
        FinalizationPolicy legacy =
                policy(
                        FinalizationPolicy
                                .LEGACY_DIRECT_TIE_BREAK_REVISION
                );

        FinalizationPolicy contextual =
                policy(
                        FinalizationPolicy
                                .CONTEXTUAL_TIE_BREAK_REVISION
                );

        assertTrue(
                legacy.usesLegacyDirectTieBreak()
        );
        assertFalse(
                legacy.usesContextualTieBreakQuestions()
        );

        assertFalse(
                contextual.usesLegacyDirectTieBreak()
        );
        assertTrue(
                contextual.usesContextualTieBreakQuestions()
        );
    }

    @Test
    void rejectsUnsupportedRevision() {
        assertThrows(
                IllegalArgumentException.class,
                () -> policy(
                        "v3"
                )
        );
    }

    private FinalizationPolicy policy(
            String revision
    ) {
        return new FinalizationPolicy(
                FinalizationPolicyType
                        .QUESTIONNAIRE_WITH_OPTIONAL_CLARIFICATION,
                revision
        );
    }
}
