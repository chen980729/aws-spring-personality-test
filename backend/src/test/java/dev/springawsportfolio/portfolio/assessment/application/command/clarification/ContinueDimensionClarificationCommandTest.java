package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContinueDimensionClarificationCommandTest {

    @Test
    void countsUnicodeCodePointsRatherThanUtf16Units() {
        String emoji =
                "🙂";

        String accepted =
                emoji.repeat(
                        ClarificationRuntimeMessage
                                .MAX_USER_ANSWER_CODE_POINTS
                );

        ContinueDimensionClarificationCommand command =
                new ContinueDimensionClarificationCommand(
                        new UserId(
                                UUID.randomUUID()
                        ),
                        AssessmentSessionId.newId(),
                        new DimensionCode(
                                "EI"
                        ),
                        1,
                        accepted
                );

        assertEquals(
                ClarificationRuntimeMessage
                        .MAX_USER_ANSWER_CODE_POINTS,
                command.answer()
                        .codePointCount(
                                0,
                                command.answer().length()
                        )
        );
    }

    @Test
    void rejectsAnswerBeyondMaximumCodePointLength() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ContinueDimensionClarificationCommand(
                                new UserId(
                                        UUID.randomUUID()
                                ),
                                AssessmentSessionId.newId(),
                                new DimensionCode(
                                        "EI"
                                ),
                                1,
                                "a".repeat(
                                        ClarificationRuntimeMessage
                                                .MAX_USER_ANSWER_CODE_POINTS
                                                + 1
                                )
                        )
        );
    }
}
