package dev.springawsportfolio.portfolio.assessment.application.clarification.runtime;

import java.time.Instant;
import java.util.Objects;

public record ClarificationRuntimeMessage(
        int sequenceNumber,
        Role role,
        String text,
        Instant createdAt,
        Instant expiresAt
) {

    public static final int MAX_ASSISTANT_QUESTION_CODE_POINTS =
            500;

    public static final int MAX_USER_ANSWER_CODE_POINTS =
            1000;

    public ClarificationRuntimeMessage {
        if (sequenceNumber <= 0) {
            throw new IllegalArgumentException(
                    "sequenceNumber must be positive"
            );
        }

        Objects.requireNonNull(
                role,
                "role must not be null"
        );

        Objects.requireNonNull(
                text,
                "text must not be null"
        );

        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    "text must not be blank"
            );
        }

        int codePoints =
                text.codePointCount(
                        0,
                        text.length()
                );

        int maximumCodePoints =
                role == Role.ASSISTANT_QUESTION
                        ? MAX_ASSISTANT_QUESTION_CODE_POINTS
                        : MAX_USER_ANSWER_CODE_POINTS;

        if (codePoints > maximumCodePoints) {
            throw new IllegalArgumentException(
                    "text exceeds maximum code point length for "
                            + role
            );
        }

        Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        Objects.requireNonNull(
                expiresAt,
                "expiresAt must not be null"
        );

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after createdAt"
            );
        }

        Role expectedRole =
                sequenceNumber % 2 == 1
                        ? Role.ASSISTANT_QUESTION
                        : Role.USER_ANSWER;

        if (role != expectedRole) {
            throw new IllegalArgumentException(
                    "odd sequence numbers must be "
                            + "ASSISTANT_QUESTION and even sequence "
                            + "numbers must be USER_ANSWER"
            );
        }
    }

    public enum Role {
        ASSISTANT_QUESTION,
        USER_ANSWER
    }
}
