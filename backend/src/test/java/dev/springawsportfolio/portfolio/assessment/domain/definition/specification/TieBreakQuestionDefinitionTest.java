package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TieBreakQuestionDefinitionTest {

    @Test
    void acceptsTwoDistinctOptionsResolvingToDifferentPoles() {
        assertDoesNotThrow(
                () -> question(
                        List.of(
                                option(
                                        "TB-EI-01",
                                        "E"
                                ),
                                option(
                                        "TB-EI-02",
                                        "I"
                                )
                        )
                )
        );
    }

    @Test
    void rejectsQuestionWithoutExactlyTwoOptions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> question(
                        List.of(
                                option(
                                        "TB-EI-01",
                                        "E"
                                )
                        )
                )
        );
    }

    @Test
    void rejectsDuplicateOptionIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> question(
                        List.of(
                                option(
                                        "TB-EI-01",
                                        "E"
                                ),
                                option(
                                        "TB-EI-01",
                                        "I"
                                )
                        )
                )
        );
    }

    @Test
    void rejectsOptionsResolvingToSamePole() {
        assertThrows(
                IllegalArgumentException.class,
                () -> question(
                        List.of(
                                option(
                                        "TB-EI-01",
                                        "E"
                                ),
                                option(
                                        "TB-EI-02",
                                        "E"
                                )
                        )
                )
        );
    }

    private TieBreakQuestionDefinition question(
            List<TieBreakOptionDefinition> options
    ) {
        return new TieBreakQuestionDefinition(
                new TieBreakQuestionId(
                        "TB-EI-1"
                ),
                new DimensionCode(
                        "EI"
                ),
                "Choose the option that feels more natural.",
                "Which approach helps your thoughts become clear?",
                options
        );
    }

    private TieBreakOptionDefinition option(
            String optionId,
            String resolvedPole
    ) {
        return new TieBreakOptionDefinition(
                new TieBreakOptionId(
                        optionId
                ),
                "Option " + optionId,
                new PoleCode(
                        resolvedPole
                )
        );
    }
}
