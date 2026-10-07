package dev.springawsportfolio.portfolio.assessment.application.clarification.runtime;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClarificationRuntimeContextTest {

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-10-07T10:00:00Z"
            );

    private static final Instant EXPIRES_AT =
            Instant.parse(
                    "2026-10-07T11:00:00Z"
            );

    @Test
    void acceptsContiguousAlternatingConversation() {
        ClarificationRuntimeContext context =
                new ClarificationRuntimeContext(
                        List.of(
                                assistantQuestion(
                                        1,
                                        "How do you recharge?"
                                ),
                                userAnswer(
                                        2,
                                        "Usually alone."
                                )
                        )
                );

        assertEquals(
                2,
                context.latestSequence()
        );
    }

    @Test
    void emptyContextIsValid() {
        ClarificationRuntimeContext context =
                ClarificationRuntimeContext.empty();

        assertTrue(
                context.isEmpty()
        );

        assertEquals(
                0,
                context.latestSequence()
        );
    }

    @Test
    void rejectsNonContiguousConversation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationRuntimeContext(
                        List.of(
                                assistantQuestion(
                                        1,
                                        "First question"
                                ),
                                new ClarificationRuntimeMessage(
                                        4,
                                        ClarificationRuntimeMessage.Role
                                                .USER_ANSWER,
                                        "Gap in sequence",
                                        CREATED_AT,
                                        EXPIRES_AT
                                )
                        )
                )
        );
    }

    @Test
    void rejectsMixedExecutionExpiry() {
        ClarificationRuntimeMessage first =
                assistantQuestion(
                        1,
                        "First question"
                );

        ClarificationRuntimeMessage second =
                new ClarificationRuntimeMessage(
                        2,
                        ClarificationRuntimeMessage.Role.USER_ANSWER,
                        "Answer",
                        CREATED_AT.plusSeconds(30),
                        EXPIRES_AT.plusSeconds(60)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationRuntimeContext(
                        List.of(
                                first,
                                second
                        )
                )
        );
    }

    @Test
    void defensivelyCopiesMessages() {
        List<ClarificationRuntimeMessage> mutable =
                new ArrayList<>();

        mutable.add(
                assistantQuestion(
                        1,
                        "Question"
                )
        );

        ClarificationRuntimeContext context =
                new ClarificationRuntimeContext(
                        mutable
                );

        mutable.clear();

        assertEquals(
                1,
                context.messages().size()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> context.messages()
                        .add(
                                assistantQuestion(
                                        1,
                                        "Another"
                                )
                        )
        );
    }

    @Test
    void messageRejectsRoleSequenceMismatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationRuntimeMessage(
                        1,
                        ClarificationRuntimeMessage.Role.USER_ANSWER,
                        "Wrong role",
                        CREATED_AT,
                        EXPIRES_AT
                )
        );
    }

    private ClarificationRuntimeMessage assistantQuestion(
            int sequence,
            String text
    ) {
        return new ClarificationRuntimeMessage(
                sequence,
                ClarificationRuntimeMessage.Role.ASSISTANT_QUESTION,
                text,
                CREATED_AT,
                EXPIRES_AT
        );
    }

    private ClarificationRuntimeMessage userAnswer(
            int sequence,
            String text
    ) {
        return new ClarificationRuntimeMessage(
                sequence,
                ClarificationRuntimeMessage.Role.USER_ANSWER,
                text,
                CREATED_AT.plusSeconds(30),
                EXPIRES_AT
        );
    }
}
