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
