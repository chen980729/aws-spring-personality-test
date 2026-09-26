package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoringPolicyTest {

    @Test
    void calculatesMaximumAbsoluteRawScore() {
        ScoringPolicy policy =
                new ScoringPolicy(
                        ScoringPolicyType
                                .CENTERED_BALANCED_KEYING,
                        "v1",
                        3,
                        1,
                        5,
                        12
                );

        assertEquals(
                24,
                policy.maximumAbsoluteRawScore()
        );
    }

    @Test
    void rejectsAsymmetricAnswerRangeForCenteredBalancedKeying() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScoringPolicy(
                        ScoringPolicyType
                                .CENTERED_BALANCED_KEYING,
                        "v1",
                        2,
                        1,
                        5,
                        12
                )
        );
    }
}