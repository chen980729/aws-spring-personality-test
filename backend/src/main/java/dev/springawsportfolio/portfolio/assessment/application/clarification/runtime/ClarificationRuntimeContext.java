package dev.springawsportfolio.portfolio.assessment.application.clarification.runtime;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ClarificationRuntimeContext(
        List<ClarificationRuntimeMessage> messages
) {

    public ClarificationRuntimeContext {
        Objects.requireNonNull(
                messages,
                "messages must not be null"
        );

        messages.forEach(message ->
                Objects.requireNonNull(
                        message,
                        "messages must not contain null"
                )
        );

        messages = List.copyOf(messages);

        Instant executionExpiry =
                messages.isEmpty()
                        ? null
                        : messages
                        .getFirst()
                        .expiresAt();

        for (
                int index = 0;
                index < messages.size();
                index++
        ) {
            ClarificationRuntimeMessage message =
                    messages.get(index);

            int expectedSequence =
                    index + 1;

            if (
                    message.sequenceNumber()
                            != expectedSequence
            ) {
                throw new IllegalArgumentException(
                        "runtime messages must be contiguous "
                                + "from sequence 1"
                );
            }

            if (
                    !message.expiresAt()
                            .equals(executionExpiry)
            ) {
                throw new IllegalArgumentException(
                        "runtime messages in one execution "
                                + "must share one expiry"
                );
            }
        }
    }

    public static ClarificationRuntimeContext empty() {
        return new ClarificationRuntimeContext(
                List.of()
        );
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public int latestSequence() {
        return messages.size();
    }
}
