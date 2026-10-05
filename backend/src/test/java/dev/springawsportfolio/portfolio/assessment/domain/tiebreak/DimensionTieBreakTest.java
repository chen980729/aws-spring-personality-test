package dev.springawsportfolio.portfolio.assessment.domain.tiebreak;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakOptionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakQuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionTieBreakTest {

    @Test
    void legacySelectionContainsNoContextualIds() {
        DimensionTieBreak tieBreak =
                new DimensionTieBreak(
                        AssessmentSessionId.newId(),
                        new DimensionCode("EI"),
                        new PoleCode("I"),
                        Instant.parse(
                                "2026-10-05T00:00:00Z"
                        )
                );

        assertTrue(
                tieBreak.isLegacyDirectSelection()
        );

        assertFalse(
                tieBreak.isContextualQuestionSelection()
        );

        assertNull(
                tieBreak.questionId()
        );

        assertNull(
                tieBreak.selectedOptionId()
        );
    }

    @Test
    void contextualSelectionRequiresQuestionAndOptionTogether() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DimensionTieBreak(
                        AssessmentSessionId.newId(),
                        new DimensionCode("EI"),
                        new TieBreakQuestionId(
                                "TB-EI-1"
                        ),
                        null,
                        new PoleCode("I"),
                        Instant.parse(
                                "2026-10-05T00:00:00Z"
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new DimensionTieBreak(
                        AssessmentSessionId.newId(),
                        new DimensionCode("EI"),
                        null,
                        new TieBreakOptionId(
                                "TB-EI-02"
                        ),
                        new PoleCode("I"),
                        Instant.parse(
                                "2026-10-05T00:00:00Z"
                        )
                )
        );
    }
}
