package dev.springawsportfolio.portfolio.assessment.application.port.out;

import java.util.Objects;

public class ClarificationAiTechnicalException
        extends RuntimeException {

    public ClarificationAiTechnicalException(
            String message
    ) {
        super(
                requireMessage(message)
        );
    }

    public ClarificationAiTechnicalException(
            String message,
            Throwable cause
    ) {
        super(
                requireMessage(message),
                Objects.requireNonNull(
                        cause,
                        "cause must not be null"
                )
        );
    }

    private static String requireMessage(
            String message
    ) {
        Objects.requireNonNull(
                message,
                "message must not be null"
        );

        if (message.isBlank()) {
            throw new IllegalArgumentException(
                    "message must not be blank"
            );
        }

        return message;
    }
}
